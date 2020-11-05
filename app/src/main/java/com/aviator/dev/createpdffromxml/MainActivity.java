package com.aviator.dev.createpdffromxml;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import com.aviator.dev.createpdffromxml.model.PDFModel;
import com.aviator.dev.createpdffromxml.utils.PDFCreationUtils;
import com.aviator.dev.createpdffromxml.utils.PdfBitmapCache;
import com.aviator.dev.createpdffromxml.utils.PrintPic;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mazenrashed.printooth.Printooth;
import com.mazenrashed.printooth.data.PrintingImagesHelper;
import com.mazenrashed.printooth.data.converter.Converter;
import com.mazenrashed.printooth.data.printable.ImagePrintable;
import com.mazenrashed.printooth.data.printable.Printable;
import com.mazenrashed.printooth.data.printable.RawPrintable;
import com.mazenrashed.printooth.data.printable.TextPrintable;
import com.mazenrashed.printooth.data.printer.DefaultPrinter;
import com.mazenrashed.printooth.data.printer.Printer;
import com.mazenrashed.printooth.ui.ScanningActivity;
import com.mazenrashed.printooth.utilities.Printing;
import com.mazenrashed.printooth.utilities.PrintingCallback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vipul.hp_hp.library.Layout_to_Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private boolean IS_MANY_PDF_FILE;

    /**
     * This is identify to number of pdf file. If pdf model list size > sector so we have create many file. After that we have merge all pdf file into one pdf file
     */
    private int SECTOR = 100; // Default value for one pdf file.
    private int START;
    private int END = SECTOR;
    private int NO_OF_PDF_FILE = 1;
    private int NO_OF_FILE;
    private int LIST_SIZE;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Printooth.INSTANCE.hasPairedPrinter()) {
            printing = Printooth.INSTANCE.printer();
            printing.setPrintingCallback(printingCallback);
        }


        initViews();

        setSupportActionBar(toolbar);


//        shopName.setText("AVIATOR SHOP");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission();
                saveAsImage();
            }
        });
    }

    private CoordinatorLayout parent;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private LinearLayout scroll;
    private TextView shopName;

    public void initViews() {
        parent = findViewById(R.id.parent);
        toolbar = findViewById(R.id.toolbar);
        fab = findViewById(R.id.fab);
        scroll = findViewById(R.id.scroll);
        shopName = findViewById(R.id.shopName);
    }


    File imgfile;

    private void saveAsImage() {
        Layout_to_Image layout_to_image;
        Bitmap bitmap;
        layout_to_image = new Layout_to_Image(MainActivity.this, scroll);
        bitmap = layout_to_image.convert_layout();
        imgfile = new File(createImgPath());
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(imgfile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            Toast.makeText(this, "Printing...", Toast.LENGTH_SHORT).show();

            startPrinting();
//            Intent intent=new Intent(this,ImagePreview.class);
//            intent.putExtra("IMG",imgfile.getPath());
//            startActivity(intent);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startPrinting() {
        if (printing != null) {
//            Printooth.INSTANCE.removeCurrentPrinter();
            printSomeImages1();
            return;
        }

        startActivityForResult(new Intent(this, ScanningActivity.class),
                ScanningActivity.SCANNING_FOR_PRINTER);
    }

    private void saveAsImage2() {
        View view = LayoutInflater.from(this).inflate(R.layout.pdf_viewer, null, false);
        view.setDrawingCacheEnabled(true);
        view.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        ));
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        view.buildDrawingCache();

        Bitmap bm = view.getDrawingCache();
//        Bitmap bitmap=Bitmap.createBitmap(bm);
        File file = new File(createImgPath());
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String createImgPath() {
        String foldername = "pdf_creation_by_xml";
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + foldername);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");

        String date = simpleDateFormat.format(Calendar.getInstance().getTime());

        return folder + File.separator + "IMG_" + date + ".jpg";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            printSomeImages1();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generatePdfReport();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScanningActivity.SCANNING_FOR_PRINTER && resultCode == Activity.RESULT_OK)
            printSomeImages1();
    }

    private Printable getPrintable(@NonNull String text, byte spacing, boolean isBold) {
        final byte bold = 0x1b; // new byte[]{0x1b, 0x45, 0x01};// 选择加粗模式
        return new TextPrintable.Builder()
                .setText(text)
                .setAlignment(DefaultPrinter.Companion.getALIGNMENT_LEFT())
                .setEmphasizedMode(isBold ? bold : DefaultPrinter.Companion.getEMPHASIZED_MODE_NORMAL()) //Bold or normal
                .setFontSize(DefaultPrinter.Companion.getFONT_SIZE_NORMAL())
                .setLineSpacing(spacing)
//                .setNewLinesAfter(1) // To provid
                .build();
    }


    byte spacing = 5;

    private void printSomeImages1() {
        List<Printable> printableList = new ArrayList<>();
        printableList.add(getPrintable("Agent: MILELE POINT\nShop: TOY SHOP\nSerial NO: 2222333344445555\n2020-04-26 17:20:09", spacing, false));
        printableList.add(getPrintable("\n********************************", DefaultPrinter.Companion.getLINE_SPACING_30(), false));
        printableList.add(getPrintable("\nPIN CODE: 2222 3333 4444 5555", spacing, true));
        printableList.add(getPrintable("\n********************************", spacing, false));
        printableList.add(getPrintable("\nKES: 20\nMobile Operator: Safaricom", DefaultPrinter.Companion.getLINE_SPACING_30(), false));
        printableList.add(getPrintable("\nThe quickest way to Top up:", spacing, false));
        printableList.add(getPrintable("\nkey in *144# followed by pin code digits and #", spacing, false));
        printableList.add(getPrintable("\nPowered By MILELE POINT", DefaultPrinter.Companion.getLINE_SPACING_30(), false));

        ArrayList<Printable> printables = new ArrayList<>();
//        printables.add(new RawPrintable.Builder(new byte[]{27, 100, 4}).build());
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.saf_print_two);
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), 200, false);
//        Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
        //Add image
        printables.add(new ImagePrintable.Builder(resized).build());

        printables.addAll(printableList);
        if (printing == null) {
            if (Printooth.INSTANCE.hasPairedPrinter()) {
                printing = Printooth.INSTANCE.printer();
                printing.setPrintingCallback(printingCallback);
            } else {
                Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        printing.print(printables);
    }

    private void printSomeImages() {
        if (imgfile != null) {
            ArrayList<Printable> printables = new ArrayList<>();
            Picasso.get()
                    .load(imgfile)
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            printables.add(new ImagePrintable.Builder(bitmap).build());
                            if (printing == null) {
                                if (Printooth.INSTANCE.hasPairedPrinter()) {
                                    printing = Printooth.INSTANCE.printer();
                                    printing.setPrintingCallback(printingCallback);
                                } else {
                                    //scan and pair
                                    startPrinting();
                                    return;
                                }
                            }

                            printing.print(printables);
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            Toast.makeText(MainActivity.this, "Failed, please try again", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            Toast.makeText(MainActivity.this, "Failed, please try again", Toast.LENGTH_SHORT).show();
                        }
                    });

//                BufferedInputStream inputStream = new BufferedInputStream(
//                        new FileInputStream(imgfile)
//                );
            //FileInputStream(imgfile);
////                printables.add(new RawPrintable.Builder(new byte[]{27, 100, 4}).build());
//                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
//                Printable printable = new ImagePrintable.Builder(resized)
//                        .setNewLinesAfter(1)
//                        .build();
//                //Add image
//                printables.add(printable);//add(new ImagePrintable.Builder(resized).build());
//                if (printing == null) {
//                    if (Printooth.INSTANCE.hasPairedPrinter()) {
//                        printing = Printooth.INSTANCE.printer();
//                        printing.setPrintingCallback(printingCallback);
//                    } else {
//                        //scan and pair
//                        startPrinting();
//                        return;
//                    }
//                }
//
//                printing.print(printables);
//                Printooth.INSTANCE.printer(new MyPrinter()).print(printables);
//                inputStream.close();
            return;
        }
        Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
    }

    private void requestPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "perms", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
        } else {
            Toast.makeText(this, "Starting", Toast.LENGTH_SHORT).show();
//            generatePdfReport();
//            saveAsImage();
            startPrinting();
        }

    }

    /**
     * This is manage to all model
     */
    private void generatePdfReport() {
        // NO_OF_FILE : This is identify to one file or many file have to created

        LIST_SIZE = 1;// pdfModels.size();
        NO_OF_FILE = LIST_SIZE / SECTOR;
        if (LIST_SIZE % SECTOR != 0) {
            NO_OF_FILE++;
        }
        if (LIST_SIZE > SECTOR) {
            IS_MANY_PDF_FILE = true;
        } else {
            END = LIST_SIZE;
        }
        createPDFFile();
    }

    private void createProgressBarForPDFCreation(int maxProgress) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(String.format("%s", String.valueOf(maxProgress)));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(maxProgress);
        progressDialog.show();
    }

    private void createProgressBarForMergePDF() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Merge pdf");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }


    /**
     * This function call with recursion
     * This recursion depend on number of file (NO_OF_PDF_FILE)
     */
    private void createPDFFile() {

        // Find sub list for per pdf file data
//        List<PDFModel> pdfDataList = pdfModels.subList(START, END);
        PdfBitmapCache.clearMemory();
        PdfBitmapCache.initBitmapCache(getApplicationContext());
        final PDFCreationUtils pdfCreationUtils = new PDFCreationUtils(MainActivity.this, 1, NO_OF_PDF_FILE);
        if (NO_OF_PDF_FILE == 1) {
            createProgressBarForPDFCreation(PDFCreationUtils.TOTAL_PROGRESS_BAR);
        }
        pdfCreationUtils.createPDF(new PDFCreationUtils.PDFCallback() {

            @Override
            public void onProgress(final int i) {
                progressDialog.setProgress(i);
            }

            @Override
            public void onCreateEveryPdfFile() {
                // Execute may pdf files and this is depend on NO_OF_FILE
                if (IS_MANY_PDF_FILE) {
                    NO_OF_PDF_FILE++;
                    if (NO_OF_FILE == NO_OF_PDF_FILE - 1) {

                        progressDialog.dismiss();
                        createProgressBarForMergePDF();
                        pdfCreationUtils.downloadAndCombinePDFs();
                    } else {

                        // This is identify to manage sub list of current pdf model list data with START and END

                        START = END;
                        if (LIST_SIZE % SECTOR != 0) {
                            if (NO_OF_FILE == NO_OF_PDF_FILE) {
                                END = (START - SECTOR) + LIST_SIZE % SECTOR;
                            }
                        }
                        END = SECTOR + END;
                        createPDFFile();
                    }

                } else {
                    // Merge one pdf file when all file is downloaded
                    progressDialog.dismiss();

                    createProgressBarForMergePDF();
                    pdfCreationUtils.downloadAndCombinePDFs();
                }

            }

            @Override
            public void onComplete(final String filePath) {
                progressDialog.dismiss();

                if (filePath != null) {
//                    btnPdfPath.setVisibility(View.VISIBLE);
//                    btnPdfPath.setText("PDF path : " + filePath);
                    Toast.makeText(MainActivity.this, "pdf file " + filePath, Toast.LENGTH_LONG).show();
//                    btnSharePdfFile.setVisibility(View.VISIBLE);
//                    btnSharePdfFile.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            sharePdf(filePath);
//                        }
//                    });
                    Intent intent = new Intent(MainActivity.this, PdfViewer.class);
                    intent.putExtra("FILEPATH", filePath);
                    MainActivity.this.startActivity(intent);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error  " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    PrintingCallback printingCallback = new PrintingCallback() {
        @Override
        public void connectingWithPrinter() {
            Toast.makeText(getApplicationContext(), "Connecting with printer", Toast.LENGTH_SHORT).show();
            Log.d("xxx", "Connecting");
        }

        @Override
        public void printingOrderSentSuccessfully() {
            Toast.makeText(getApplicationContext(), "printingOrderSentSuccessfully", Toast.LENGTH_SHORT).show();
            Log.d("xxx", "printingOrderSentSuccessfully");
        }

        @Override
        public void connectionFailed(@NonNull String error) {
            Toast.makeText(getApplicationContext(), "connectionFailed :" + error, Toast.LENGTH_SHORT).show();
            Log.d("xxx", "connectionFailed : " + error);
        }

        @Override
        public void onError(@NonNull String error) {
            Toast.makeText(getApplicationContext(), "onError :" + error, Toast.LENGTH_SHORT).show();
            Log.d("xxx", "onError : " + error);
        }

        @Override
        public void onMessage(@NonNull String message) {
            Toast.makeText(getApplicationContext(), "onMessage :" + message, Toast.LENGTH_SHORT).show();
            Log.d("xxx", "onMessage : " + message);
        }
    };

    Printing printing;

    static class MyPrinter extends Printer {


        @NonNull
        @Override
        public PrintingImagesHelper initPrintingImagesHelper() {
            return new Pp();
        }


        @NonNull
        @Override
        public byte[] initCharacterCodeCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initEmphasizedModeCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initFeedLineCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initFontSizeCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initInitPrinterCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initJustificationCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initLineSpacingCommand() {
            return new byte[0];
        }


        @NonNull
        @Override
        public byte[] initUnderlineModeCommand() {
            return new byte[0];
        }

        @NonNull
        @Override
        public Converter useConverter() {
            return new Converter() {
                @Override
                protected String convert(String input) {
                    return super.convert(input);
                }
            };
        }
    }

    static class Pp implements PrintingImagesHelper {
        @NonNull
        @Override
        public byte[] getBitmapAsByteArray(@NonNull Bitmap bitmap) {
            PrintPic printPic = PrintPic.getInstance();
            printPic.init(bitmap);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            return printPic.printDraw();
        }
    }
}
