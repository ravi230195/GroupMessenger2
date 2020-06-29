package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.PriorityQueue;

public class testClass {
    public static void testMessageClass()
    {
        MessageBody messageBody = new MessageBody("ravi", 1, 1);
        messageBody.printMessage();
        String ser = messageBody.serialize();
        System.out.println(ser);
        MessageBody desMes = MessageBody.deserilize(ser);
        desMes.printMessage();
    }
}

/*
import java.io.*;
import java.util.Random;
import java.util.*;
 class MessageBody {
    static final String TAG = "MessageBody: ";
    enum messageType
    {
        MULTICAST,
        PROPOSAL,
        DELIVERY,
        ERROR
    }

    public String      msg;
    public messageType msgType;
    public int         message_unique;
    public int         process_orgin;
    public int         process_dest;
    public int         msgProposedSeq;
    public int         orginMsgSeq;



    MessageBody(String msg, int process_orgin, int orginMsgSeq)
    {
        this.msg = msg;
        this.msgType = messageType.MULTICAST;
        this.process_orgin = process_orgin;
        this.orginMsgSeq = orginMsgSeq;
        this.message_unique = hashCodeGenerator(msg);
    }

    public void printMessage()
    {

         System.out.println(serialize());
                 }

                 int hashCodeGenerator(String msg)
                 {
                 Random rand = new Random();
                 int val = msg.hashCode() + rand.nextInt(10000);
                 return val;
                 }

public String serialize()
        {
        String ser = "";
        ser = ser + this.msg + ":";
        ser = ser + this.msgType.toString() + ":";
        ser = ser + this.message_unique + ":";
        ser = ser + this.process_orgin + ":";
        ser = ser + this.process_dest + ":";
        ser = ser + this.msgProposedSeq + ":";
        ser = ser + this.orginMsgSeq + "\n";
        return ser;
        }

public static messageType getMsgType(String msgType)
        {
        switch (messageType.valueOf(msgType))
        {
        case MULTICAST:
        return messageType.MULTICAST;
        case PROPOSAL:
        return messageType.PROPOSAL;
        case DELIVERY:
        return messageType.DELIVERY;
default: {
        //System.out.println(TAG,"Something Went Wrong");
        return messageType.ERROR;
        }
        }
        }


        }

class myComparator implements Comparator<MessageBody> {

    // Overriding compare()method of Comparator
    // for descending order of cgpa
    public int compare(MessageBody s1, MessageBody s2) {
        if (s1.msgProposedSeq > s2.msgProposedSeq)
            return 1;
        else if (s1.msgProposedSeq < s2.msgProposedSeq)
            return -1;
        else {
            if (s1.process_dest < s2.process_dest)
                return 1;
            else if (s1.process_dest > s2.process_dest)
                return -1;
        }
        return 0;
    }
}


public class HelloWorld{

    public static void main(String []args){
        System.out.println("Hello World");

        MessageBody messageBody = new MessageBody("ravi", 11120, 13);
        messageBody.process_dest = 11124;
        messageBody.printMessage();
        MessageBody messageBody1 = new MessageBody("ravi", 11112, 13);
        messageBody1.process_dest = 11120;
        messageBody1.printMessage();

        PriorityQueue<MessageBody> priorityQueue = new PriorityQueue<MessageBody>(100, new myComparator());
        priorityQueue.add(messageBody);
        priorityQueue.add(messageBody1);
        while(priorityQueue.size() != 0)
        {
            MessageBody messageBodyTemp = priorityQueue.poll();
            messageBodyTemp.printMessage();
        }
    }
}
 */