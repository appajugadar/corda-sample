package com.cts.corda.etf.util;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Random;

@Slf4j
public class JMSPublisher {

    static public void sendMessage(String message, String queueName) throws JMSException {
        //ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost:61616?broker.persistent=true,useShutdownHook=false");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://DESKTOP-7M2VMIM:61616");

        Connection connection=null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(queueName);
            //Setup a message producer to send message to the queue the server is consuming from
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            Destination tempDest = session.createTemporaryQueue();
            //Now create the actual message you want to send
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText(message);
            txtMessage.setJMSReplyTo(tempDest);
            String correlationId = createRandomString();
            txtMessage.setJMSCorrelationID(correlationId);
            producer.send(txtMessage);
            log.info("Message sent to "+queueName);
        }  finally {
            connection.close();
        }
    }

    private static String createRandomString() {
        return Long.toHexString(new Random(System.currentTimeMillis()).nextLong());
    }
}