/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.store.jdbc;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.activeio.command.WireFormat;
import org.apache.activeio.util.FactoryFinder;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.memory.UsageManager;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.store.MessageStore;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.TopicMessageStore;
import org.apache.activemq.store.TransactionStore;
import org.apache.activemq.store.jdbc.adapter.DefaultJDBCAdapter;
import org.apache.activemq.store.memory.MemoryTransactionStore;
import org.apache.activemq.util.IOExceptionSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.emory.mathcs.backport.java.util.concurrent.ScheduledFuture;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A {@link PersistenceAdapter} implementation using JDBC for persistence
 * storage.
 * 
 * This persistence adapter will correctly remember prepared XA transactions,
 * but it will not keep track of local transaction commits so that operations
 * performed against the Message store are done as a single uow.
 * 
 * @org.apache.xbean.XBean element="jdbcPersistenceAdapter"
 * 
 * @version $Revision: 1.9 $
 */
public class JDBCPersistenceAdapter implements PersistenceAdapter {

    private static final Log log = LogFactory.getLog(JDBCPersistenceAdapter.class);
    private static FactoryFinder factoryFinder = new FactoryFinder("META-INF/services/org/apache/activemq/store/jdbc/");

    private WireFormat wireFormat = new OpenWireFormat();
    private DataSource dataSource;
    private Statements statements;
    private JDBCAdapter adapter;
    private MemoryTransactionStore transactionStore;
    private ScheduledThreadPoolExecutor clockDaemon;
    private ScheduledFuture clockTicket;
    int cleanupPeriod = 1000 * 60 * 5;
    private boolean useExternalMessageReferences;

    public JDBCPersistenceAdapter() {
    }

    public JDBCPersistenceAdapter(DataSource ds, WireFormat wireFormat) {
        this.dataSource = ds;
        this.wireFormat = wireFormat;
    }

    public Set getDestinations() {
        // Get a connection and insert the message into the DB.
        TransactionContext c = getTransactionContext();
        try {
            return getAdapter().doGetDestinations(c);
        } catch (IOException e) {
            return Collections.EMPTY_SET;
        } catch (SQLException e) {
            JDBCPersistenceAdapter.log("JDBC Failure: ",e);
            return Collections.EMPTY_SET;
        } finally {
            try {
                c.close();
            } catch (Throwable e) {
            }
        }
    }

    public MessageStore createQueueMessageStore(ActiveMQQueue destination) throws IOException {
        MessageStore rc = new JDBCMessageStore(this, getAdapter(), wireFormat, destination);
        if (transactionStore != null) {
            rc = transactionStore.proxy(rc);
        }
        return rc;
    }

    public TopicMessageStore createTopicMessageStore(ActiveMQTopic destination) throws IOException {
        TopicMessageStore rc = new JDBCTopicMessageStore(this, getAdapter(), wireFormat, destination);
        if (transactionStore != null) {
            rc = transactionStore.proxy(rc);
        }
        return rc;
    }

    public TransactionStore createTransactionStore() throws IOException {
        if (transactionStore == null) {
            transactionStore = new MemoryTransactionStore();
        }
        return this.transactionStore;
    }

    public long getLastMessageBrokerSequenceId() throws IOException {
        // Get a connection and insert the message into the DB.
        TransactionContext c = getTransactionContext();
        try {
            return getAdapter().doGetLastMessageBrokerSequenceId(c);
        } catch (SQLException e) {
            JDBCPersistenceAdapter.log("JDBC Failure: ",e);
            throw IOExceptionSupport.create("Failed to get last broker message id: " + e, e);
        } finally {
            c.close();
        }
    }

    public void start() throws Exception {
        
        getAdapter().setUseExternalMessageReferences(isUseExternalMessageReferences());

        TransactionContext transactionContext = getTransactionContext();
        transactionContext.begin();
        try {
            try {
                getAdapter().doCreateTables(transactionContext);
            } catch (SQLException e) {
                log.warn("Cannot create tables due to: " + e);
                JDBCPersistenceAdapter.log("Failure Details: ",e);
            }
        } finally {
            transactionContext.commit();
        }

        cleanup();

        // Cleanup the db periodically.
        if (cleanupPeriod > 0) {
            clockTicket = getScheduledThreadPoolExecutor().scheduleAtFixedRate(new Runnable() {
                public void run() {
                    cleanup();
                }
            }, cleanupPeriod, cleanupPeriod, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() throws Exception {
        if (clockTicket != null) {
            clockTicket.cancel(true);
            clockTicket = null;
            clockDaemon.shutdown();
        }
    }

    public void cleanup() {
        TransactionContext c = getTransactionContext();
        try {
            log.debug("Cleaning up old messages.");
            c = getTransactionContext();
            getAdapter().doDeleteOldMessages(c);
        } catch (IOException e) {
            log.warn("Old message cleanup failed due to: " + e, e);
        } catch (SQLException e) {
            log.warn("Old message cleanup failed due to: " + e);
            JDBCPersistenceAdapter.log("Failure Details: ",e);
        } finally {
            try {
                c.close();
            } catch (Throwable e) {
            }
            log.debug("Cleanup done.");
        }
    }

    public void setScheduledThreadPoolExecutor(ScheduledThreadPoolExecutor clockDaemon) {
        this.clockDaemon = clockDaemon;
    }

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        if (clockDaemon == null) {
            clockDaemon = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "ActiveMQ Cleanup Timer");
                    thread.setDaemon(true);
                    return thread;
                }
            });
        }
        return clockDaemon;
    }

    public JDBCAdapter getAdapter() throws IOException {
        if (adapter == null) {
            setAdapter(createAdapter());
        }
        return adapter;
    }

    /**
     * @throws IOException
     */
    protected JDBCAdapter createAdapter() throws IOException {
        JDBCAdapter adapter=null;
        TransactionContext c = getTransactionContext();
        try {

            try {

                // Make the filename file system safe.
                String dirverName = c.getConnection().getMetaData().getDriverName();
                dirverName = dirverName.replaceAll("[^a-zA-Z0-9\\-]", "_").toLowerCase();

                try {
                    adapter = (DefaultJDBCAdapter) factoryFinder.newInstance(dirverName);
                    log.info("Database driver recognized: [" + dirverName + "]");
                } catch (Throwable e) {
                    log.warn("Database driver NOT recognized: [" + dirverName
                            + "].  Will use default JDBC implementation.");
                }

            } catch (SQLException e) {
                log.warn("JDBC error occurred while trying to detect database type.  Will use default JDBC implementation: "
                                + e.getMessage());
                JDBCPersistenceAdapter.log("Failure Details: ",e);
            }

            // Use the default JDBC adapter if the
            // Database type is not recognized.
            if (adapter == null) {
                adapter = new DefaultJDBCAdapter();
            }

        } finally {
            c.close();
        }
        return adapter;
    }

    public void setAdapter(JDBCAdapter adapter) {
        this.adapter = adapter;
        this.adapter.setStatements(getStatements());
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public WireFormat getWireFormat() {
        return wireFormat;
    }

    public void setWireFormat(WireFormat wireFormat) {
        this.wireFormat = wireFormat;
    }

    public TransactionContext getTransactionContext(ConnectionContext context) {
        if (context == null) {
            return getTransactionContext();
        } else {
            TransactionContext answer = (TransactionContext) context.getLongTermStoreContext();
            if (answer == null) {
                answer = new TransactionContext(dataSource);
                context.setLongTermStoreContext(answer);
            }
            return answer;
        }
    }

    public TransactionContext getTransactionContext() {
        return new TransactionContext(dataSource);
    }

    public void beginTransaction(ConnectionContext context) throws IOException {
        TransactionContext transactionContext = getTransactionContext(context);
        transactionContext.begin();
    }

    public void commitTransaction(ConnectionContext context) throws IOException {
        TransactionContext transactionContext = getTransactionContext(context);
        transactionContext.commit();
    }

    public void rollbackTransaction(ConnectionContext context) throws IOException {
        TransactionContext transactionContext = getTransactionContext(context);
        transactionContext.rollback();
    }

    public int getCleanupPeriod() {
        return cleanupPeriod;
    }

    public void setCleanupPeriod(int cleanupPeriod) {
        this.cleanupPeriod = cleanupPeriod;
    }

    public void deleteAllMessages() throws IOException {
        TransactionContext c = getTransactionContext();
        try {
            getAdapter().doDropTables(c);
            getAdapter().setUseExternalMessageReferences(isUseExternalMessageReferences());
            getAdapter().doCreateTables(c);
        } catch (SQLException e) {
            JDBCPersistenceAdapter.log("JDBC Failure: ",e);
            throw IOExceptionSupport.create(e);
        } finally {
            c.close();
        }
    }

    public boolean isUseExternalMessageReferences() {
        return useExternalMessageReferences;
    }

    public void setUseExternalMessageReferences(boolean useExternalMessageReferences) {
        this.useExternalMessageReferences = useExternalMessageReferences;
    }
    
    static public void log(String msg, SQLException e) {
        String s = msg+e.getMessage();
        while( e.getNextException() != null ) {
            e = e.getNextException();
            s += ", due to: "+e.getMessage();
        }
        log.debug(s, e);
    }

    public Statements getStatements() {
        if( statements == null ) {
            statements = new Statements();
        }
        return statements;
    }

    public void setStatements(Statements statements) {
        this.statements = statements;
    }

    /**
     * @param usageManager The UsageManager that is controlling the destination's memory usage.
     */
    public void setUsageManager(UsageManager usageManager) {
    }

}
