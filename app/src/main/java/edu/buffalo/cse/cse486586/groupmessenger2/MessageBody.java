package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

import java.io.*;
import java.util.Random;

public class MessageBody {
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

    MessageBody()
    {

    }
    public MessageBody clone()
    {
        MessageBody messageBodyNew = new MessageBody();
        messageBodyNew.msg = this.msg;
        messageBodyNew.msgType = this.msgType;
        messageBodyNew.process_orgin = this.process_orgin;
        messageBodyNew.orginMsgSeq = this.orginMsgSeq;
        messageBodyNew.message_unique = this.message_unique;
        messageBodyNew.process_dest = this.process_dest;
        messageBodyNew.msgProposedSeq = this.msgProposedSeq;
        return messageBodyNew;
    }
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
        /*
        Log.e(TAG,"-------------------------------------------" );
        Log.e(TAG,"test Message " +  msg);
        Log.e(TAG,"Message Type " + msgType.toString());
        Log.e(TAG,"Message Unique Id " + message_unique);
        Log.e(TAG,"Sender Process " + process_orgin);
        Log.e(TAG,"Destination Process " + process_dest);
        Log.e(TAG,"Proposal Value " + msgProposedSeq);
        Log.e(TAG,"Sender Origin seq " + orginMsgSeq);
        Log.e(TAG,"-------------------------------------------" );
        */
        Log.e(TAG,serialize());
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
                Log.e(TAG,"Something Went Wrong");
                return messageType.ERROR;
            }
        }
    }

    public static MessageBody deserilize(String ser)
    {
        String[] des = ser.split(":");

        MessageBody messageBody = new MessageBody();
        messageBody.msg = des[0];
        messageBody.msgType = getMsgType(des[1]);
        messageBody.message_unique = Integer.parseInt(des[2]);
        messageBody.process_orgin = Integer.parseInt(des[3]);
        messageBody.process_dest = Integer.parseInt(des[4]);
        messageBody.msgProposedSeq = Integer.parseInt(des[5]);
        messageBody.orginMsgSeq = Integer.parseInt(des[6]);
        return messageBody;
    }
}
