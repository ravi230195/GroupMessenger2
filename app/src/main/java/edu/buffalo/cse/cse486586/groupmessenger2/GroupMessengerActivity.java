package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.util.*;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

class myComparator implements Comparator<MessageBody> {

    // Overriding compare()method of Comparator
    // for descending order of cgpa
    public int compare(MessageBody s1, MessageBody s2) {
        if (s1.msgProposedSeq > s2.msgProposedSeq)
            return 1;
        else if (s1.msgProposedSeq < s2.msgProposedSeq)
            return -1;
        else {
            if (s1.process_dest > s2.process_dest)
                return 1;
            else if (s1.process_dest < s2.process_dest)
                return -1;
        }
        return 0;
    }
}



public class GroupMessengerActivity extends Activity {
    static final int SERVER_PORT = 10000;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    public  static CopyOnWriteArrayList<Integer> REMOTE_PORTS =  new CopyOnWriteArrayList<Integer>(Arrays.asList(11108, 11112, 11116,11120,11124));
    private Uri uri;
    public  int senderMsgSeq = 0;
    public  int seqNumber = 0;
    public  int agreedSeq =  0;
    public  int myPort;
    public int ordering = 0;
    public int TIMEOUT = 1000;
    public  HashSet<Integer> myMsg = new HashSet<Integer>();
    public  static HashMap<Integer, MessageBody> priorityMap = new HashMap<Integer, MessageBody>();
    public HashMap<Integer, HashSet<Integer>> receivedProposal = new HashMap<Integer, HashSet<Integer>>();
    public  static PriorityQueue<MessageBody> priorityQueue = new PriorityQueue<MessageBody>(100, new myComparator());
    public LinkedList<MessageBody> hold_proposal =  new LinkedList<MessageBody>();
    public HashMap<Integer, Integer> FIFO = new HashMap<Integer, Integer>();


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myport = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.e(TAG, "myport "+ myPort);
        FIFO.put(11108,0);
        FIFO.put(11112,0);
        FIFO.put(11116,0);
        FIFO.put(11120,0);
        FIFO.put(11124,0);
        myPort = Integer.parseInt(myport);
        Log.e(TAG, "myPort "+ myPort + " " + myport);
        try {

            Log.e(TAG,"check");

            ServerSocket serverSocket = new ServerSocket(); // <-- create an unbound socket first
            //serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket" + e.getMessage().toString());
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)) {

                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        String msg = editText.getText().toString() + "\n";
                        editText.setText(""); // This is one way to reset the input box.

                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                        return true;
                    }
                }
                return false;
            }
        });


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         *
         */

        final Button button = (Button) findViewById(R.id.button4);
        synchronized (this) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "OnClickCalled");
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    public  int getSequence() {
        senderMsgSeq++;
        //Log.e(TAG,(String)senderMsgSeq);
        return senderMsgSeq;
    }

    public  synchronized MessageBody  createProposal(MessageBody messageBody) {
        MessageBody messageBodyNew = messageBody.clone();
        seqNumber = seqNumber > agreedSeq ? seqNumber:agreedSeq;
        ++seqNumber;
        messageBodyNew.msgProposedSeq = seqNumber;
        messageBodyNew.process_dest = myPort;
        messageBodyNew.msgType = MessageBody.messageType.PROPOSAL;
        return messageBodyNew;
    }

    public  void sending(MessageBody messageBody, int port) {
        new HandleSending(messageBody, port,2);
    }

    public void sendProposalMsg(MessageBody messageBody) {
        try {
            new HandleSending(messageBody,0,3);
        } catch (Exception e) {
            Log.e(TAG, "sendProposalMsg: Error"+ e.getCause() + e.getClass());
        }

    }

    public   synchronized  void  updateHashQueue(MessageBody messageBody, boolean remove) {
        try {
            if (remove) {
                priorityQueue.remove(messageBody);
                priorityMap.remove(messageBody.message_unique);
            } else {
                priorityQueue.add(messageBody);
                priorityMap.put(messageBody.message_unique, messageBody);
                checkForDelivery();
            }

        }
        catch (Exception e)
        {
            Log.e(TAG,"updateHashQueue " + e.getClass()+e.getMessage());

        }
    }

    public  synchronized  void  pickWinner(MessageBody messageBody) {
            MessageBody messageBodyTemp = priorityMap.get(messageBody.message_unique);
            MessageBody messageBodyNew = messageBodyTemp.clone();

            messageBodyNew.msgType = MessageBody.messageType.DELIVERY;
            updateHashQueue(messageBodyTemp, true);
            updateHashQueue(messageBodyNew, false);
            agreedSeq = agreedSeq > messageBodyNew.msgProposedSeq ? agreedSeq : messageBodyNew.msgProposedSeq;
            receivedProposal.remove(messageBody.message_unique);
            sending(messageBodyNew, myPort);


    }

    void print(PriorityQueue<MessageBody> messageBodyPriorityQueue) {

        Iterator<MessageBody> itr = messageBodyPriorityQueue.iterator();
        while (itr.hasNext()) {
            Log.e(TAG, "Queue: ||||||||||||||||||" + itr.next().serialize());
        }
    }

    class Display extends ServerTask
    {
        public void dispaly(String str)
        {
            publishProgress(str);
        }
    }

    public synchronized  void checkForDelivery() {

            try {
                while (true) {
                    Log.e(TAG, "checkForDelivery: Priority print");
                    print(priorityQueue);
                    MessageBody messageBody = priorityQueue.peek();
                    if (messageBody != null) {
                        if (messageBody.msgType == MessageBody.messageType.DELIVERY) {
                            Log.e(TAG, "Delivering message " + myPort + " " + messageBody.process_orgin + messageBody.serialize());
                            ContentValues content = new ContentValues();
                            content.put("key", Integer.toString(ordering));
                            content.put("value", messageBody.msg);
                            getContentResolver().insert(uri, content);

                            ordering++;
                            updateHashQueue(messageBody, true);
                            new Display().dispaly(("Deivery " + ordering + "  " + messageBody.serialize()));
                        } else
                            return;
                    } else
                        return;
                }
            } catch (Exception e) {
                Log.e(TAG, "checkForDelivery " + e.getClass() + e.getMessage());
            }

    }

    public synchronized void handleFailureServer(MessageBody messageBody, int port) {
        try {
            if (REMOTE_PORTS.contains(port)) {
                Log.e(TAG, "handleFailure server: Entered here");
                    // Confirmed ADV Down.
                    Log.e(TAG, "updateRemoteList: updating...remove " + port);
                    REMOTE_PORTS.remove(new Integer(port));
                    checkQueueForDeadMessages(port);
            }
            else
            {
                Log.e(TAG, "handleFailure: " + " portAlreadyRemove");
            }
        }

        catch (Exception e)
        {
            Log.e(TAG, "handleFailue: "+ e.getMessage() + e.getClass());
        }
    }




    void updateRemoteList(Integer port)
    {

            Log.e(TAG, "updateRemoteList: updating...remove " + port);
            REMOTE_PORTS.remove(new Integer(port));


    }

    synchronized  void checkQueueForDeadMessages(int deadPort) {
        try {
            Log.e(TAG, "checkQueueForDeadMessages: ented ");
            print(priorityQueue);
            Iterator<MessageBody> itr = priorityQueue.iterator();
            while (itr.hasNext()) {
                MessageBody messageBodyCheck = itr.next();
                if (messageBodyCheck.msgType != MessageBody.messageType.DELIVERY) {
                    if (messageBodyCheck.process_orgin == deadPort) {
                        Log.e(TAG, "checkQueueForDeadMessages: removed message " + messageBodyCheck.serialize());
                        itr.remove();
                    }
                }
            }
            itr = priorityQueue.iterator();
            while (itr.hasNext()) {
                MessageBody messageBodyCheck = itr.next();
                if (messageBodyCheck.msgType != MessageBody.messageType.DELIVERY) {
                        if (messageBodyCheck.process_orgin == myPort) {
                            // check if the proposal message is pending from dead process
                            // if so mark it for delivery as the process is dead.
                            HashSet<Integer> recivedPorts = receivedProposal.get(messageBodyCheck.message_unique);
                            if (recivedPorts.contains(deadPort)) {
                                //remove it from the list and check for delivery
                                recivedPorts.remove(deadPort);
                            }
                            if (recivedPorts.size() >= REMOTE_PORTS.size()) {
                                pickWinner(messageBodyCheck);
                            }
                        }
                    }
                }
            }
        catch (Exception e)
        {
            Log.e(TAG, "checkQueueForDeadMessages: " + e.getClass() + e.getMessage());
        }
    }

    // ---------------------SERVER CODE---------------------------------

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        //int ordering = 0;


        public int compareInsert(MessageBody old, MessageBody newM) {
            if (old.msgProposedSeq > newM.msgProposedSeq)
                return -1;
            else if (old.msgProposedSeq < newM.msgProposedSeq)
                return 1;
            else {
                if (old.process_dest < newM.process_dest)
                    return 1;
                else if (old.process_dest > newM.process_dest)
                    return -1;
            }
            return 0;
        }

        public void checkAndInsert(MessageBody messageBody) {
            try {
                MessageBody messageBodyTemp = priorityMap.get(messageBody.message_unique);
                if (messageBodyTemp != null) {
                    if (compareInsert(messageBodyTemp, messageBody) == 1) {
                        updateHashQueue(messageBodyTemp, true);
                        updateHashQueue(messageBody, false);
                    }
                    if (receivedProposal.get(messageBody.message_unique).size() >= REMOTE_PORTS.size()) {
                        Log.e(TAG, "PickingWinner");
                        pickWinner(messageBody);
                    }
                } else {
                    updateHashQueue(messageBody, false);
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "checkAndInsert: "+ e.getMessage() + e.getClass());
            }

        }

        void printRecivedProposal(HashMap<Integer, HashSet<Integer>> value) {
            for (Integer i : value.keySet()) {
                Log.e(TAG, "printRecivedProposal: ####################" + i + value.get(i));
            }
        }

        public  void updatePropsalRecived(MessageBody messageBody) {
            try {
                int count = receivedProposal.containsKey(messageBody.message_unique) ? receivedProposal.get(messageBody.message_unique).size() : 0;
                if (count == 0)
                {
                    HashSet<Integer> dest = new HashSet<Integer>();
                    dest.add(messageBody.process_dest);
                    receivedProposal.put(messageBody.message_unique, dest);
                }
                else {
                    receivedProposal.get(messageBody.message_unique).add(messageBody.process_dest);
                }
                checkAndInsert(messageBody);
                printRecivedProposal(receivedProposal);
            }
            catch (Exception e)
            {
                Log.e(TAG, "updatePropsalRecived: "+ e.getMessage() + e.getClass());
            }
        }

        public void handleMultiCast(MessageBody messageBody) {
            messageBody.printMessage();
            MessageBody messageBodynew = createProposal(messageBody);
            if (myPort != messageBodynew.process_orgin) {
                sendProposalMsg(messageBodynew);
                updateHashQueue(messageBodynew, false);
            }
            else
            {
                updatePropsalRecived(messageBodynew);
            }

        }

        void checkForAnypendingProposals(MessageBody messageBody) {
            try {
                Iterator<MessageBody> iterator = hold_proposal.iterator();
                while (iterator.hasNext()) {
                    MessageBody messageBodyTemp = iterator.next();
                    if (messageBody.process_orgin == messageBodyTemp.process_orgin) {
                        if (messageBodyTemp.orginMsgSeq == FIFO.get(messageBodyTemp.process_orgin) + 1) {
                            handleMultiCast(messageBodyTemp);
                            FIFO.put(messageBodyTemp.process_orgin, messageBodyTemp.orginMsgSeq + 1);
                            iterator.remove();
                        }
                    }

                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "checkForAnypendingProposals: "+e.getMessage() + e.getClass() );
            }
        }

        public void handleIncomingMessages(MessageBody messageBody, Socket soc) {
            try {

                if (messageBody != null) {
                    if (messageBody.msg.equals("FAIL"))
                    {
                       handleFailureServer(messageBody, messageBody.process_dest);
                    }

                    else if (messageBody.msgType == MessageBody.messageType.MULTICAST && REMOTE_PORTS.contains(messageBody.process_orgin)) {

                        if (messageBody.orginMsgSeq == FIFO.get(messageBody.process_orgin) + 1) {
                            handleMultiCast(messageBody);
                            FIFO.put(messageBody.process_orgin, messageBody.orginMsgSeq);
                            checkForAnypendingProposals(messageBody);
                        }
                        else
                        {
                            hold_proposal.add(messageBody);
                        }
                    } else if (messageBody.msgType == MessageBody.messageType.PROPOSAL && REMOTE_PORTS.contains(messageBody.process_dest)) {
                        messageBody.printMessage();
                        updatePropsalRecived(messageBody);
                    } else if (messageBody.msgType == MessageBody.messageType.DELIVERY && REMOTE_PORTS.contains(messageBody.process_orgin)) {
                        if (!priorityMap.containsKey(messageBody.message_unique)) {
                            Log.e(TAG,"Something went wrong");
                        } else {
                            MessageBody messageBodyTemp = priorityMap.get(messageBody.message_unique);
                            //priorityQueue.remove(messageBodyTemp);
                            Log.e(TAG,"DELIVED MSG ***********" + messageBody.serialize());
                            updateHashQueue(messageBodyTemp, true);
                            updateHashQueue(messageBody, false);
                            agreedSeq = agreedSeq > messageBody.msgProposedSeq? agreedSeq:messageBody.msgProposedSeq;
                            Log.e(TAG,"Agreed seqNumber" + agreedSeq + " Proposal Seqnumber " + seqNumber);
                        }
                    }
                    else
                    {
                        Log.e(TAG, "handleIncomingMessages: " + " Message droped as that AVD is down " );
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,"handleIncoming " + e.getClass()+e.getMessage());
            }
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            //new MessageProcessor();
            //new MessageDeliver();
            try
            {
                Log.e(TAG, "entering Server");
                while (true) {
                    try {
                        Log.e(TAG, "run: Check Server ......for Incoming");
                        Socket soc = serverSocket.accept();
                        soc.setTcpNoDelay(true);
                        //soc.setSoTimeout(TIMEOUT);
                        //Log.e(TAG, "Message Received");
                        BufferedReader in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                        String des = in.readLine();
                        Log.e(TAG, "Message received " + des);
                        //TimeUnit.MILLISECONDS.sleep(1);
                        String ser = "ACK\n";

                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
                        bufferedWriter.write(ser);
                        bufferedWriter.flush();
                        String ping = "PING";
                        if (des != null && !des.equals(ping)) {
                            MessageBody messageBody = MessageBody.deserilize(des);

                            handleIncomingMessages(messageBody, soc);
                        }
                        //Log.e(TAG, "doInBackground ser: printing priority");
                        //print(priorityQueue);

                    }
                    catch (Exception e) {
                        Log.e(TAG, "doInBackground server: Error occured" + e.getCause() + e.getClass());
                    }

                }
            }catch (Exception e){
                ;
                Log.e(TAG, "doInBackground server: Error occured" + e.getCause() + e.getClass());
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }



    // -----------------------CLIENT CODE-----------------------------------
    class HandleSending implements Runnable {
        String ack = "ACK";
        private MessageBody messageBody;
        private int port;
        private  int type;

        public  synchronized void handleFailure(MessageBody messageBody, Exception exception, int port, Iterator<Integer> sendingiter) {
            try {
                if (REMOTE_PORTS.contains(port)) {
                    Log.e(TAG, "handleFailure: Entered here");
                    if (exception instanceof SocketTimeoutException) {
                        // Not reliable check with ping if its Down
                        Boolean isAlive = startPingProtocal(messageBody, port);
                        if (isAlive) {
                            // do nothing continue;
                        } else {
                            String failure = "FAIL";
                            MessageBody fail = new MessageBody(failure, myPort, -1);
                            fail.process_dest = port;
                            new HandleSending(fail, port,2);

                        }
                    } else
                    {
                        // Confirmed ADV Down.
                        String failure = "FAIL";
                        MessageBody fail = new MessageBody(failure, myPort, -1);
                        fail.process_dest = port;
                        Log.e(TAG, "handleFailure: " + fail.serialize() );
                        new HandleSending(fail, port,2);
                    }
                }
                else
                {
                    Log.e(TAG, "handleFailure: " + " portAlreadyRemove");
                }
            }

            catch (Exception e)
            {
                Log.e(TAG, "handleFailue: "+ e.getMessage() + e.getClass());
            }
        }

        public synchronized Boolean startPingProtocal(MessageBody messageBody, int port) {
            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
                socket.setSoTimeout(TIMEOUT);
                String ser = "PING\n";
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                bufferedWriter.write(ser);
                bufferedWriter.flush();
                String ack = "ACK";
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String des = in.readLine();
                if (ack.equals(des)) {
                    Log.e(TAG, "sendingALL: Confirmation recived ALIVE");
                    socket.close();
                    return true;
                }
            } catch (SocketTimeoutException e)
            {

                Log.e(TAG, "startPingProtocal SocketTimeoutException" + e.getMessage());
                Log.e(TAG, "startPingProtocal SocketTimeoutException Increase Timeout" + e.getMessage());
                return false;

            }
            catch (UnknownHostException e) {
                Log.e(TAG, "startPingProtocal UnknownHostException" + e.getMessage());
                return false;
            } catch (IOException e) {

                Log.e(TAG, "startPingProtocal socket IOException" + e.getMessage());
                return false;
            }
            return false;
        }

        public  void sendingALL(MessageBody messageBody) {
            if (messageBody != null) {
                String ser = messageBody.serialize();

                //for ( int i = 0 ; i < REMOTE_PORTS.size(); i++) {
                    Iterator<Integer> iter = REMOTE_PORTS.iterator();
                    ArrayList<Integer> toRemove = new ArrayList<Integer>();
                    while(iter.hasNext()) {
                        int sendingport = iter.next();
                        try {

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingport);
                            //socket.setSoTimeout(TIMEOUT);
                            Log.e(TAG, "sendingALL: Sending to sendingALL sending " + sendingport);
                            //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            //out.println(ser);

                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            bufferedWriter.write(ser);
                            bufferedWriter.flush();

                            String ack = "ACK";
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String des = in.readLine();
                            if (des.equals(ack)) {
                                Log.e(TAG, "sendingALL: Confirmation recived for " + messageBody.serialize()  + "from  "+ sendingport);
                                //TimeUnit.MILLISECONDS.sleep(1);
                                socket.close();
                            }


                        } catch (SocketTimeoutException e) {

                            Log.e(TAG, "sendingALL UnknownHostException" + e.getMessage());
                            toRemove.add(sendingport);
                            handleFailure(messageBody, e,sendingport,iter );
                        } catch (UnknownHostException e) {

                            Log.e(TAG, "sendingALL UnknownHostException" + e.getMessage());
                            toRemove.add(sendingport);
                            handleFailure(messageBody, e,sendingport,iter );
                        } catch (IOException e) {

                            Log.e(TAG, "sendingALL socket IOException" + e.getMessage());
                            toRemove.add(sendingport);
                            handleFailure(messageBody, e,sendingport,iter );
                        } catch (Exception e) {

                            Log.e(TAG, "sendToOnlyOnly socket Exception" + e.getMessage());
                            toRemove.add(sendingport);
                            handleFailure(messageBody, e,sendingport,iter );
                        }
                    }/*
                        for (int i = 0; i < toRemove.size(); i++)
                        {
                            handleFailure(messageBody, new Exception(), toRemove.get(i), null );
                        }*/

            }
        }


        public void sendExcept(MessageBody messageBody, int port) {
            if (messageBody != null) {
                String ser = messageBody.serialize();

                //for ( int i = 0 ; i < REMOTE_PORTS.size(); i++) {
                Iterator<Integer> iter = REMOTE_PORTS.iterator();
                ArrayList<Integer> toRemove = new ArrayList<Integer>();
                while(iter.hasNext())
                {
                    int sendingport = iter.next();
                    try {

                        if (port != sendingport) {

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), sendingport);
                            //socket.setSoTimeout(TIMEOUT);
                            Log.e(TAG, "sendExcept: Sending to sendExcept sending " + sendingport );
                            //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            //out.println(ser);

                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            bufferedWriter.write(ser);
                            bufferedWriter.flush();

                            String ack = "ACK";
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String des = in.readLine();
                            if (des.equals(ack)) {
                                Log.e(TAG, "sendExcept: Confirmation recived for " + messageBody.serialize()  + "from  "+ sendingport);
                                //TimeUnit.MILLISECONDS.sleep(1);
                                socket.close();
                            }

                        }
                    }catch (SocketTimeoutException e)
                    {

                        Log.e(TAG, "sendExcept UnknownHostException " + e.getMessage());
                        toRemove.add(sendingport);
                        handleFailure(messageBody, e,sendingport,iter );
                    }

                    catch (UnknownHostException e) {

                        Log.e(TAG, "sendExcept UnknownHostException " + e.getMessage());
                        toRemove.add(sendingport);
                        handleFailure(messageBody, e,sendingport,iter );
                    }
                    catch (IOException e) {

                        Log.e(TAG, "sendExcept socket IOException " + e.getMessage());
                        toRemove.add(sendingport);
                        handleFailure(messageBody, e,sendingport,iter );
                    }
                    catch (Exception e) {

                        Log.e(TAG, "sendToOnlyOnly socket Exception " + e.getMessage());
                        toRemove.add(sendingport);
                        handleFailure(messageBody, e,sendingport,iter );
                    }
                }/*
                for (int i = 0; i < toRemove.size(); i++)
                {
                    handleFailure(messageBody, new Exception(), toRemove.get(i), null );
                }*/
            }
        }


        public void sendToOnlyOnly(MessageBody messageBody) {
            if (messageBody != null) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), messageBody.process_orgin);
                    //socket.setSoTimeout(TIMEOUT);
                    Log.e(TAG, "sendToOnlyOnly: Sending to sendToOnlyOnly " + messageBody.process_orgin );
                    //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String ser = messageBody.serialize();
                    Log.e(TAG, "sending from proposal sendToOnlyOnly " + ser);
                    //out.println(ser);

                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    bufferedWriter.write(ser);
                    bufferedWriter.flush();


                    String ack = "ACK";
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String des = in.readLine();
                    //Log.e(TAG, "sendToOnlyOnly: recived" + des );
                    if (des.equals(ack)) {
                        Log.e(TAG, "sendToOnlyOnly: Confirmation recived for " + messageBody.serialize() + "from  "+ messageBody.process_orgin);
                        //TimeUnit.MILLISECONDS.sleep(1);
                        socket.close();
                    }
                } catch (SocketTimeoutException e)
                {

                    Log.e(TAG, "sendToOnlyOnly UnknownHostException " + e.getMessage());
                    handleFailure(messageBody, e, messageBody.process_orgin , null);
                }

                catch (UnknownHostException e) {

                    Log.e(TAG, "sendToOnlyOnly UnknownHostException " + e.getMessage());
                    handleFailure(messageBody, e, messageBody.process_orgin,null);
                }
                catch (IOException e) {

                    Log.e(TAG, "sendToOnlyOnly socket IOException " + e.getMessage());
                    handleFailure(messageBody, e, messageBody.process_orgin,null);
                }
                catch (Exception e) {

                    Log.e(TAG, "sendToOnlyOnly socket Exception" + e.getMessage());
                    handleFailure(messageBody, e, messageBody.process_orgin,null);
                }
            }
        }

        HandleSending(MessageBody messageBody, int port, int type) {
            this.messageBody = messageBody;
            this.port = port;
            this.type = type;
            Thread thread = new Thread(this);
            thread.start();
        }

        public void run() {
            if (type == 1) {
                sendingALL(messageBody);
            }
            else if (type == 2)
            {
                sendExcept(messageBody, port);
            }
            else
            {
                sendToOnlyOnly(messageBody);
            }
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msg = msgs[0].trim();
            MessageBody messageBody = new MessageBody(msg, myPort, getSequence());
            Log.e(TAG,"Creating MSG-------");
            myMsg.add(messageBody.message_unique);
            messageBody.printMessage();
            new HandleSending(messageBody, (0), 1);
            return null;
        }
    }


}
