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
package org.apache.activemq.broker.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jms.InvalidClientIDException;
import javax.jms.JMSException;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.Connection;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.BrokerId;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.MessageDispatchNotification;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.command.RemoveSubscriptionInfo;
import org.apache.activemq.command.SessionInfo;
import org.apache.activemq.command.TransactionId;
import org.apache.activemq.memory.UsageManager;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.activemq.thread.TaskRunnerFactory;
import org.apache.activemq.util.IdGenerator;
import org.apache.activemq.util.LongSequenceGenerator;
import org.apache.activemq.util.ServiceStopper;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArraySet;

/**
 * Routes Broker operations to the correct messaging regions for processing.
 * 
 * @version $Revision$
 */
public class RegionBroker implements Broker {

    private static final IdGenerator brokerIdGenerator = new IdGenerator();

    private final Region queueRegion;
    private final Region topicRegion;
    private final Region tempQueueRegion;
    private final Region tempTopicRegion;
    private BrokerService brokerService;
    private boolean stopped = false;
    private boolean keepDurableSubsActive=false;
    
    protected final DestinationStatistics destinationStatistics = new DestinationStatistics();
    
    private final CopyOnWriteArrayList connections = new CopyOnWriteArrayList();
    private final CopyOnWriteArraySet destinations = new CopyOnWriteArraySet();
    private final CopyOnWriteArrayList brokerInfos = new CopyOnWriteArrayList();

    private final LongSequenceGenerator sequenceGenerator = new LongSequenceGenerator();    
    private BrokerId brokerId;
    private String brokerName;
    private Map clientIdSet = new HashMap(); // we will synchronize access
    protected  PersistenceAdapter adaptor;
    
    protected final ConcurrentHashMap connectionStates = new ConcurrentHashMap();

        
    public RegionBroker(BrokerService brokerService,TaskRunnerFactory taskRunnerFactory, UsageManager memoryManager, PersistenceAdapter adapter) throws IOException {
        this.brokerService = brokerService;
        this.sequenceGenerator.setLastSequenceId( adapter.getLastMessageBrokerSequenceId() );
        this.adaptor = adapter;//weird - both are valid spellings ...
        queueRegion = createQueueRegion(memoryManager, taskRunnerFactory, adapter);
        topicRegion = createTopicRegion(memoryManager, taskRunnerFactory, adapter);
        
        tempQueueRegion = createTempQueueRegion(memoryManager, taskRunnerFactory);
        tempTopicRegion = createTempTopicRegion(memoryManager, taskRunnerFactory);        
    }
    
    public Map getDestinationMap() {
        Map answer = getQueueRegion().getDestinationMap();
        answer.putAll(getTopicRegion().getDestinationMap());
        return answer;
    }

    public Broker getAdaptor(Class type){
        if (type.isInstance(this)){
            return this;
        }
        return null;
    }

    public Region getQueueRegion() {
        return queueRegion;
    }

    public Region getTempQueueRegion() {
        return tempQueueRegion;
    }

    public Region getTempTopicRegion() {
        return tempTopicRegion;
    }

    public Region getTopicRegion() {
        return topicRegion;
    }

    protected Region createTempTopicRegion(UsageManager memoryManager, TaskRunnerFactory taskRunnerFactory) {
        return new TempTopicRegion(this,destinationStatistics, memoryManager, taskRunnerFactory);
    }

    protected Region createTempQueueRegion(UsageManager memoryManager, TaskRunnerFactory taskRunnerFactory) {
        return new TempQueueRegion(this,destinationStatistics, memoryManager, taskRunnerFactory);
    }

    protected Region createTopicRegion(UsageManager memoryManager, TaskRunnerFactory taskRunnerFactory, PersistenceAdapter adapter) {
        return new TopicRegion(this,destinationStatistics, memoryManager, taskRunnerFactory, adapter);
    }

    protected Region createQueueRegion(UsageManager memoryManager, TaskRunnerFactory taskRunnerFactory, PersistenceAdapter adapter) {
        return new QueueRegion(this,destinationStatistics, memoryManager, taskRunnerFactory, adapter);
    }
    
    private static PersistenceAdapter createDefaultPersistenceAdapter(UsageManager memoryManager) throws IOException {
        return new MemoryPersistenceAdapter();
    }
    
    
    public void start() throws Exception {
        ((TopicRegion)topicRegion).setKeepDurableSubsActive(keepDurableSubsActive);
        queueRegion.start();
        topicRegion.start();
        tempQueueRegion.start();
        tempTopicRegion.start();
    }

    public void stop() throws Exception {
        stopped = true;
        ServiceStopper ss = new ServiceStopper();
        doStop(ss);
        ss.throwFirstException();
    }
    
    public PolicyMap getDestinationPolicy(){
        return brokerService != null ? brokerService.getDestinationPolicy() : null;
    }

    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        String clientId = info.getClientId();
        if (clientId == null) {
            throw new InvalidClientIDException("No clientID specified for connection request");
        }
        synchronized (clientIdSet ) {
            if (clientIdSet.containsKey(clientId)) {
                throw new InvalidClientIDException("Broker: " + getBrokerName() + " - Client: " + clientId + " already connected");
            }
            else {
                clientIdSet.put(clientId, info);
            }
        }

        connections.add(context.getConnection());
    }

    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error) throws Exception {
        String clientId = info.getClientId();
        if (clientId == null) {
            throw new InvalidClientIDException("No clientID specified for connection disconnect request");
        }
        synchronized (clientIdSet) {
            ConnectionInfo oldValue = (ConnectionInfo) clientIdSet.get(clientId);
            // we may be removing the duplicate connection, not the first connection to be created
            if (oldValue == info) {
                clientIdSet.remove(clientId);
            }
        }

        connections.remove(context.getConnection());
    }

    public Connection[] getClients() throws Exception {
        ArrayList l = new ArrayList(connections);
        Connection rc[] = new Connection[l.size()];
        l.toArray(rc);
        return rc;
    }

    public Destination addDestination(ConnectionContext context, ActiveMQDestination destination) throws Exception {
        if( destinations.contains(destination) ){
            throw new JMSException("Destination already exists: "+destination);
        }
        
        Destination answer = null;
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            answer  = queueRegion.addDestination(context, destination);
            break;
        case ActiveMQDestination.TOPIC_TYPE:
            answer = topicRegion.addDestination(context, destination);
            break;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            answer = tempQueueRegion.addDestination(context, destination);
            break;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            answer = tempTopicRegion.addDestination(context, destination);
            break;
        default:
            throw createUnknownDestinationTypeException(destination);
        }

        destinations.add(destination);
        return answer;
    }

    public void removeDestination(ConnectionContext context,ActiveMQDestination destination,long timeout)
                    throws Exception{
        if(destinations.contains(destination)){
            switch(destination.getDestinationType()){
            case ActiveMQDestination.QUEUE_TYPE:
                queueRegion.removeDestination(context,destination,timeout);
                break;
            case ActiveMQDestination.TOPIC_TYPE:
                topicRegion.removeDestination(context,destination,timeout);
                break;
            case ActiveMQDestination.TEMP_QUEUE_TYPE:
                tempQueueRegion.removeDestination(context,destination,timeout);
                break;
            case ActiveMQDestination.TEMP_TOPIC_TYPE:
                tempTopicRegion.removeDestination(context,destination,timeout);
                break;
            default:
                throw createUnknownDestinationTypeException(destination);
            }
            destinations.remove(destination);
        }
    }
    
    public void addDestinationInfo(ConnectionContext context,DestinationInfo info) throws Exception{
        addDestination(context,info.getDestination());
        
    }

    public void removeDestinationInfo(ConnectionContext context,DestinationInfo info) throws Exception{
        removeDestination(context,info.getDestination(), info.getTimeout());
        
    }

    public ActiveMQDestination[] getDestinations() throws Exception {
        ArrayList l = new ArrayList(destinations);
        ActiveMQDestination rc[] = new ActiveMQDestination[l.size()];
        l.toArray(rc);
        return rc;
    }


    public void addSession(ConnectionContext context, SessionInfo info) throws Exception {
    }

    public void removeSession(ConnectionContext context, SessionInfo info) throws Exception {
    }

    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
    }

    public void removeProducer(ConnectionContext context, ProducerInfo info) throws Exception {
    }

    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        ActiveMQDestination destination = info.getDestination();
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            return queueRegion.addConsumer(context, info);
            
        case ActiveMQDestination.TOPIC_TYPE:
            return topicRegion.addConsumer(context, info);
        
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            return tempQueueRegion.addConsumer(context, info);
            
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            return tempTopicRegion.addConsumer(context, info);
            
        default:
            throw createUnknownDestinationTypeException(destination);
        }
    }

    public void removeConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        ActiveMQDestination destination = info.getDestination();
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            queueRegion.removeConsumer(context, info);
            break;
        case ActiveMQDestination.TOPIC_TYPE:
            topicRegion.removeConsumer(context, info);
            break;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            tempQueueRegion.removeConsumer(context, info);
            break;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            tempTopicRegion.removeConsumer(context, info);
            break;
        default:
            throw createUnknownDestinationTypeException(destination);
        }
    }

    public void removeSubscription(ConnectionContext context, RemoveSubscriptionInfo info) throws Exception {
        topicRegion.removeSubscription(context, info);
    }

    public void send(ConnectionContext context,  Message message) throws Exception {
        message.getMessageId().setBrokerSequenceId(sequenceGenerator.getNextSequenceId());
        if (message.getTimestamp() > 0 && (message.getBrokerPath() == null || message.getBrokerPath().length == 0)) { 
            //timestamp not been disabled and has not passed through a network
            message.setTimestamp(System.currentTimeMillis());
        }
        ActiveMQDestination destination = message.getDestination();
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            queueRegion.send(context, message);
            break;
        case ActiveMQDestination.TOPIC_TYPE:
            topicRegion.send(context, message);
            break;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            tempQueueRegion.send(context, message);
            break;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            tempTopicRegion.send(context, message);
            break;
        default:
            throw createUnknownDestinationTypeException(destination);
        }
    }

    public void acknowledge(ConnectionContext context, MessageAck ack) throws Exception {
        ActiveMQDestination destination = ack.getDestination();
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            queueRegion.acknowledge(context, ack);
            break;
        case ActiveMQDestination.TOPIC_TYPE:
            topicRegion.acknowledge(context, ack);
            break;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            tempQueueRegion.acknowledge(context, ack);
            break;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            tempTopicRegion.acknowledge(context, ack);
            break;
        default:
            throw createUnknownDestinationTypeException(destination);
        }
    }

    public TransactionId[] getPreparedTransactions(ConnectionContext context) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }

    public void beginTransaction(ConnectionContext context, TransactionId xid) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }

    public int prepareTransaction(ConnectionContext context, TransactionId xid) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }

    public void rollbackTransaction(ConnectionContext context, TransactionId xid) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }

    public void commitTransaction(ConnectionContext context, TransactionId xid, boolean onePhase) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }
    
    public void forgetTransaction(ConnectionContext context, TransactionId transactionId) throws Exception {
        throw new IllegalAccessException("Transaction operation not implemented by this broker.");
    }


    public void gc() {
        queueRegion.gc();
        topicRegion.gc();
    }

    public BrokerId getBrokerId() {
        if( brokerId==null ) {
            // TODO: this should persist the broker id so that subsequent startup
            // uses the same broker id.
            brokerId=new BrokerId(brokerIdGenerator.generateId());
        }
        return brokerId;
    }
    
    public void setBrokerId(BrokerId brokerId) {
        this.brokerId = brokerId;
    }

    public String getBrokerName() {
        if( brokerName==null ) {
            try {
                brokerName = java.net.InetAddress.getLocalHost().getHostName().toLowerCase();
            } catch (Exception e) {
                brokerName="localhost";
            }
        }
        return brokerName;
    }
    
    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }
	
    public DestinationStatistics getDestinationStatistics() {
        return destinationStatistics;
    }

    protected JMSException createUnknownDestinationTypeException(ActiveMQDestination destination) {
        return new JMSException("Unknown destination type: " + destination.getDestinationType());
    }

    public synchronized void addBroker(Connection connection,BrokerInfo info){
            brokerInfos.add(info);
    }
    
    public synchronized void removeBroker(Connection connection,BrokerInfo info){
        if (info != null){
            brokerInfos.remove(info);
        }   
    }

    public synchronized BrokerInfo[] getPeerBrokerInfos(){
        BrokerInfo[] result = new BrokerInfo[brokerInfos.size()];
        result = (BrokerInfo[])brokerInfos.toArray(result);
        return result;
    }
    
    public void processDispatch(MessageDispatch messageDispatch){
        
    }
    
    public void processDispatchNotification(MessageDispatchNotification messageDispatchNotification) throws Exception {
        ActiveMQDestination destination = messageDispatchNotification.getDestination();
        switch(destination.getDestinationType()) {
        case ActiveMQDestination.QUEUE_TYPE:
            queueRegion.processDispatchNotification(messageDispatchNotification);
            break;
        case ActiveMQDestination.TOPIC_TYPE:
            topicRegion.processDispatchNotification(messageDispatchNotification);
            break;
        case ActiveMQDestination.TEMP_QUEUE_TYPE:
            tempQueueRegion.processDispatchNotification(messageDispatchNotification);
            break;
        case ActiveMQDestination.TEMP_TOPIC_TYPE:
            tempTopicRegion.processDispatchNotification(messageDispatchNotification);
            break;
        default:
            throw createUnknownDestinationTypeException(destination);
        }
    }
    
    public boolean isSlaveBroker(){
        return brokerService.isSlave();
    }
    
    public boolean isStopped(){
        return stopped;
    }
    
    public Set getDurableDestinations(){
        return adaptor != null ? adaptor.getDestinations() : Collections.EMPTY_SET;
    }
    
    public boolean isFaultTolerantConfiguration(){
        return false;
    }


    protected void doStop(ServiceStopper ss) {
        ss.stop(queueRegion);
        ss.stop(topicRegion);
        ss.stop(tempQueueRegion);
        ss.stop(tempTopicRegion);
    }

    public boolean isKeepDurableSubsActive() {
        return keepDurableSubsActive;
    }

    public void setKeepDurableSubsActive(boolean keepDurableSubsActive) {
        this.keepDurableSubsActive = keepDurableSubsActive;
    }

	public Map getConnectionStates() {
		return connectionStates;
	}


}
