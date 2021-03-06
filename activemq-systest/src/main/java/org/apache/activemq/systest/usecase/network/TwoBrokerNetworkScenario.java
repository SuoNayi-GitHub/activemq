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
package org.apache.activemq.systest.usecase.network;

import org.apache.activemq.systest.BrokerAgent;
import org.apache.activemq.systest.ConsumerAgent;
import org.apache.activemq.systest.MessageList;
import org.apache.activemq.systest.ProducerAgent;

/**
 * 
 * @version $Revision: 1.1 $
 */
public class TwoBrokerNetworkScenario extends ProducerConsumerScenarioSupport {

    protected BrokerAgent brokerA;
    protected BrokerAgent brokerB;

    public TwoBrokerNetworkScenario(BrokerAgent brokera, BrokerAgent brokerb, ProducerAgent producer,
            ConsumerAgent consumer, MessageList list) {
        super(producer, consumer, list);
        brokerA = brokera;
        brokerB = brokerb;
    }

    public void run() throws Exception {
        producer.sendMessages(messageList);
        consumer.assertConsumed(messageList);
    }

    public void start() throws Exception {
        startBrokers();

        consumer.connectTo(brokerB);
        producer.connectTo(brokerA);

        super.start();
                
        // Wait a little bit so that the network state can be propagated. 
        Thread.sleep(1000);
    }

    protected void startBrokers() throws Exception {
        start(brokerA);
        start(brokerB);
        
        brokerB.connectTo(brokerA);
        brokerA.connectTo(brokerB);
    }
}
