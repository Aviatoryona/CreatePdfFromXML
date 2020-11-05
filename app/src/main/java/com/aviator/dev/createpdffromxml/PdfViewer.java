package com.aviator.dev.createpdffromxml;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shockwave.pdfium.PdfDocument;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PdfViewer extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket socket;
    BluetoothDevice bluetoothDevice;
    OutputStream outputStream;
    InputStream inputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    String value = "";

    File file;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfviewer);

        initViews();

//        pdfView.setScrollBar(scrollBar);
        if (getIntent().hasExtra("FILEPATH")) {
            String path = getIntent().getStringExtra("FILEPATH");

            if (path != null) {
                file = new File(path);
                pdfView.fromFile(file)
                        .defaultPage(1)
                        .onPageChange(this)
                        .swipeVertical(true)
                        .showMinimap(false)
                        .enableAnnotationRendering(true)
                        .onLoad(this)
                        .load();
            }

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    printPdf();
                }
            });
        }
    }

    String txtvalue;

    private void printPdf() {
        txtvalue = getString(R.string.lorem);
        IntentPrint();
    }

    private PDFView pdfView;
    private FloatingActionButton fab;

    public void initViews() {
        pdfView = findViewById(R.id.pdfView);
        fab = findViewById(R.id.fab);
    }


    String TAG = "MITAG";

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            InitPrinter();
        }
    }

    byte[] PrintHeader = {(byte) 0xAA, 0x55, 2, 0};

    public void IntentPrint() {
        byte[] buffer = txtvalue.getBytes();
        PrintHeader[3] = (byte) buffer.length;
        InitPrinter();
    }

    /*
     * Close the connection to bluetooth printer.
     */
    void closeBT() {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            socket.close();

            // tell the user data were sent
            Toast.makeText(this, "Data sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void sendData() {
        if (PrintHeader.length > 128) {
            value += "\nValue is more than 128 size\n";
            Toast.makeText(this, value, Toast.LENGTH_LONG).show();
        } else {
            try {

                outputStream.write(txtvalue.getBytes());

                closeBT();
            } catch (Exception ex) {
                value += ex.toString() + "\n" + "Excep IntentPrint \n";
                Toast.makeText(this, value, Toast.LENGTH_LONG).show();
            }
        }
    }

    void sendPrintPdf() {
        try {

            InputStream is = new FileInputStream(file);//this.openFileInput("filename.pdf"); // Where this is Activity
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];

            int bytesRead;
            while ((bytesRead=is.read(b)) != -1){
                bos.write(b, 0, bytesRead);
            }
            byte[] bytes = bos.toByteArray();

            byte[] printformat = {27, 33, 0}; //try adding this print format

            outputStream.write(printformat);
            outputStream.write(bytes);


            closeBT();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void InitPrinter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                //                    if(device.getName().equals("SP200")) //Note, you will need to change this to match the name of your device
                //                    {
                //                        bluetoothDevice = device;
                //                        break;
                //                    }
                bluetoothDevices.addAll(pairedDevices);

//                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
//                Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
//                socket=bluetoothDevice.createRfcommSocketToServiceRecord(uuid);;
//                socket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                bluetoothAdapter.cancelDiscovery();

                selectDevice();
//                socket.connect();
//                outputStream = socket.getOutputStream();
//                inputStream = socket.getInputStream();
//                beginListenForData();
            } else {
                value += "No Devices found";
                Toast.makeText(this, value, Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            value += ex.toString() + "\n" + " InitPrinter \n";
            Toast.makeText(this, value, Toast.LENGTH_LONG).show();
        }
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = inputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                inputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                Log.d("e", data);
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    void selectDevice() {
        AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
        aBuilder.setAdapter(new MiAdapter(bluetoothDevices), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                bluetoothDevice = bluetoothDevices.get(i);
                bindBlueTooth();
            }
        });
        aBuilder.setTitle("Select printer");
        aBuilder.setCancelable(true);
        aBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        aBuilder.show();
    }

    private void bindBlueTooth() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
//        Method m = null;
        try {
//            m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                socket=bluetoothDevice.createRfcommSocketToServiceRecord(uuid);;
//            socket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
            bluetoothAdapter.cancelDiscovery();
            socket.connect();
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            beginListenForData();
//            sendData();
            sendPrintPdf();
        } catch (IOException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            Toast.makeText(this, "Error encountered", Toast.LENGTH_SHORT).show();
        }
    }

    List<BluetoothDevice> bluetoothDevices = new ArrayList<>();

    static class MiAdapter extends BaseAdapter {
        List<BluetoothDevice> devices;

        MiAdapter(List<BluetoothDevice> devices) {
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i).getName();
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null){
                view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.mitextview,null,false);
                TextView textView = view.findViewById(R.id.txtView);
                textView.setText(String.valueOf(getItem(i)));
            }
            return view;
        }
    }
}
