package me.raid.printjartest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import me.raid.printjartest.format.FixedSpacePrintContentBuilder;
import me.raid.printjartest.format.IFormatedPrintContentBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.laiqian.print.model.IPrinterDiscoverySession;
import com.laiqian.print.model.IPrinterDiscoverySession.PrinterDiscoveryObserver;
import com.laiqian.print.model.BasePrinterDiscoverySession;
import com.laiqian.print.model.PrintContent;
import com.laiqian.print.model.PrintJobInfo;
import com.laiqian.print.model.PrintJobInfo.StatusObserver;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.model.PrintManager.PrinterConnectionResultObserver;
import com.laiqian.print.model.PrinterInfo;
import com.laiqian.print.model.type.bluetooth.BluetoothPrintManager;
import com.laiqian.print.model.type.bluetooth.BluetoothPrinterDiscoverySession;
import com.laiqian.print.model.type.net.NetPrintManager;
import com.laiqian.print.model.type.net.TcpDumpHelper;
import com.laiqian.print.model.type.serial.SerialPrinterInfo;
import com.laiqian.print.model.type.usb.UsbPrintManager;
import com.laiqian.print.util.PrintUtils;


/**
 * @author Raid
 *
 */
public class SamplePrintActivity extends Activity {

    private static class ContentView {
        public ListView lv;
        public Button button1;
        public Button button2;
        private Activity activity;

        public ContentView(Activity activity) {
            this.activity = activity;
            lv = (ListView) activity.findViewById(R.id.lv);
            button1 = (Button) activity.findViewById(R.id.btn1);
            button2 = (Button) activity.findViewById(R.id.btn2);
        }
    }

    /**
     * @author Raid_Workstation actually you cannot search serial printers
     */
    private static class SerialPrinterDiscoverySession extends BasePrinterDiscoverySession {

        @Override
        public void start() {
            SerialPrinterInfo defaultPrinter = new SerialPrinterInfo("/dev/ttyS1", 9600);

            onFoundPrinter(defaultPrinter);
        }

        @Override
        public void cancel() {

        }

    }

    PrintManager printManager;
    private ContentView contentView;
    private IPrinterDiscoverySession usbSession;
    private IPrinterDiscoverySession netSession;
    private IPrinterDiscoverySession bluetoothSession;
    private IPrinterDiscoverySession serialSession;

    private ArrayList<PrinterInfo> printers = new ArrayList<PrinterInfo>();
    private SimplePrinterAdapter adapter = null;
    private PrintContent testContent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        copyUsbDevicePropertyFile();

        setupView();
        setListeners();

        printManager = PrintManager.getInstance(this);
        printManager.setPrinterConnectionResultObserver(new PrinterConnectionResultObserver() {

            @Override
            public void onResult(String identifier, boolean result) {
                findPrinter(identifier).setConnected(result);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        TcpDumpHelper.copyFromAssetToLocal(this);
    }

    void setupView() {
        setContentView(R.layout.activity_sample_print);
        contentView = new ContentView(this);
        adapter = new SimplePrinterAdapter(this, printers);
        contentView.lv.setAdapter(adapter);
    }

    void setListeners() {
        contentView.button1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                toggleSearch();
            }
        });

        contentView.button2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                togglePrintAll();
            }
        });

        contentView.lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("tag", "onItemClick");
                adapter.notifyDataSetChanged();
                PrinterInfo printer = adapter.getItem(position);

                if (printer.isConnected()) {
                    Log.i("tag", "connected");
                    PrintJobInfo job = new PrintJobInfo(printer, getTestContent());
                    job.setStatusObserver(new StatusObserver() {

                        @Override
                        public void onStatus(PrintJobInfo job, int newStatus) {
                            if (job.isEnded()) {
                                String msg = job.getName() + " " + job.getStatusName() + "\n";
                                msg += "error message: " + job.getErrorMessage() + "\n";
                                msg += "wait " + job.getWaitTime() + "ms\n";
                                msg += "execution " + job.getExecutionTime() + "ms";
                                final String finalMsg = msg;
                                PrintUtils.runInMainThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(SamplePrintActivity.this, finalMsg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                    printManager.print(job);
                } else {
                    Log.i("tag", "disconnected, try connect");
                    printManager.connect(printer);
                }
            }

        });
    }

    private PrinterInfo findPrinter(String identifier) {
        for (PrinterInfo printer : printers) {
            if (printer.getIdentifier().equals(identifier)) {
                return printer;
            }
        }
        return null;
    }


    private String usbDevicePropertyFileName = "printer.json";

    private boolean copyUsbDevicePropertyFile() {
        boolean success = false;
        String folder = getApplicationInfo().dataDir + "/";
        File target = new File(folder + usbDevicePropertyFileName);

        AssetManager manager = this.getAssets();
        try {
            InputStream is = manager.open(usbDevicePropertyFileName);
            OutputStream os = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }
            is.close();
            os.close();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    private void togglePrintAll() {
        int size = adapter.getCount();
        for (int i = 0; i < size; ++i) {
            PrinterInfo printer = adapter.getItem(i);
            PrintJobInfo job = new PrintJobInfo(printer, getTestContent());
            job.setStatusObserver(new StatusObserver() {

                @Override
                public void onStatus(final PrintJobInfo job, int newStatus) {
                    if (job.isEnded()) {
                        String msg = job.getName() + " " + job.getStatusName() + "\n";
                        msg += "wait " + job.getWaitTime() + "ms\n";
                        msg += "execution " + job.getExecutionTime() + "ms";
                        final String finalMsg = msg;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(SamplePrintActivity.this, finalMsg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            printManager.print(job);
        }
    }

    private PrintContent getLineOnlyContent() {
        PrintContent.Builder builder = new PrintContent.Builder();

        // full length
        builder.appendString("--------------------------------");
        // half
        builder.appendString("----------------");
        // middle space
        builder.appendString("--------------- ----------------");
        // infix space
        builder.appendString("- - - - - - - - - - - - - - - - ");

        builder.appendString("双倍");
        builder.appendString("--------------------------------", PrintContent.FONT_DOUBLE_BOTH);
        builder.appendString("双高");
        builder.appendString("--------------------------------", PrintContent.FONT_DOUBLE_HEIGHT);
        builder.appendString("双宽");
        builder.appendString("--------------------------------", PrintContent.FONT_DOUBLE_WIDTH);

        builder.appendString("普通");
        builder.appendString("--------------------------------");
        builder.appendString("粗体");
        builder.appendString("--------------------------------", true, false, PrintContent.ALIGN_LEFT, false, false);

        // underline
        builder.appendString("下划线");
        builder.appendString("________________________________");
        InputStream is;
        try {
            is = getResources().getAssets().open("weixin_200_200.bmp");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            builder.appendBitmap(bmp, PrintContent.ALIGN_CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.build();
    }

    private PrintContent getTestContent() {
        if (testContent != null) {
            return testContent;
        }

        PrintContent.Builder builder = new PrintContent.Builder();
        // will print new line
        builder.appendString("Title", PrintContent.FONT_DOUBLE_BOTH, PrintContent.ALIGN_CENTER);

        Time t = new Time();
        t.setToNow();
        builder.appendString("Time: " + t.format2445(), PrintContent.FONT_NORMAL, PrintContent.ALIGN_RIGHT);
        builder.appendString("Hello, printer!", PrintContent.FONT_DOUBLE_HEIGHT);

        // Wrapper for PrintContent.Builder, providing some formating feature
        IFormatedPrintContentBuilder formatBuilder = new FixedSpacePrintContentBuilder(builder);
        formatBuilder.appendStrings(new String[] {"一二三四五六七八九一二三四五六七八九"});
        formatBuilder.appendStrings(new String[] {"1"});
        formatBuilder.appendStrings(new String[] {"2", "2"});
        formatBuilder.appendStrings(new String[] {"3", "3", "3"});
        formatBuilder.appendStrings(new String[] {"一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
                "1234567890123456789012345678901234567890"});
        formatBuilder.appendStrings(new String[] {"4", "4", "4", "4"});
        formatBuilder.appendStrings(new String[] {"一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
                "1234567890123456789012345678901234567890", "1234567890123456789012345678901234567890"});
        builder.appendString("");
        builder.appendString("");
        builder.appendString("");

        // print Bitmap
        InputStream is;
        try {
            is = getResources().getAssets().open("weixin_200_200.bmp");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            builder.appendBitmap(bmp, PrintContent.ALIGN_CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // cash drawer pulse signal
        builder.appendPulseSignal();

        builder.appendString("");
        builder.appendString("");
        builder.appendString("");


        formatBuilder.appendStrings(new String[] {"一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
                "1234567890123456789012345678901234567890", "1234567890123456789012345678901234567890"});
        builder.appendString("");
        builder.appendString("");
        builder.appendString("");
        formatBuilder.appendStrings(new String[] {"一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
                "1234567890123456789012345678901234567890"});

        builder.appendString("正常大小Normal");
        builder.appendString("双倍宽度Double Width", PrintContent.FONT_DOUBLE_WIDTH);
        builder.appendString("双倍高度Double Height", PrintContent.FONT_DOUBLE_HEIGHT);
        builder.appendString("双倍字体Double Size", PrintContent.FONT_DOUBLE_BOTH);
        builder.appendString("正常大小粗体Normal Bold", true, false, PrintContent.ALIGN_LEFT, false, false);
        builder.appendString("双倍宽度粗体Double Width Bold", true, false, PrintContent.ALIGN_LEFT, true, false);
        builder.appendString("双倍高度粗体Double Height Bold", true, false, PrintContent.ALIGN_LEFT, false, true);
        builder.appendString("双倍字体粗体Double Size Bold", true, false, PrintContent.ALIGN_LEFT, true, true);

        builder.appendString("");
        builder.appendString("");
        builder.appendString("");

        testContent = builder.build();
        return testContent;
    }

    private void toggleSearch() {
        if (isSearching()) {
            cancelPrinterSearch();
        } else {
            startPrinterSearch();
        }
    }

    private void cancelPrinterSearch() {
        if (usbSession != null) {
            usbSession.cancel();
        }
        if (netSession != null) {
            netSession.cancel();
        }
        if (bluetoothSession != null) {
            bluetoothSession.cancel();
        }
        if (serialSession != null) {
            serialSession.cancel();
        }
    }

    private void startPrinterSearch() {
        prepareSearch();

        if (usbSession != null) {
            usbSession.start();
        }
        if (netSession != null) {
            netSession.start();
        }
        if (bluetoothSession != null) {
            bluetoothSession.start(); // NOTE: bluetooth discovery may cause wifi disconnection
        }
        if (serialSession != null) {
            serialSession.start();
        }
    }

    private void prepareSearch() {
        clearSearchResult();

        if (UsbPrintManager.isUsbAvaliable()) {
            usbSession = printManager.openUsbPrinterDiscoverySession();
            usbSession.setObserver(generalObserver);
        } else {
            Toast.makeText(this, "USB function not avaliable, system version lower than 3.0", Toast.LENGTH_SHORT)
                    .show();
        }

        if (NetPrintManager.isWifiEnabled(this)) {
            netSession = printManager.openNetPrinterDiscoverySession();
            netSession.setObserver(generalObserver);
        } else {
            Toast.makeText(this, "WIFI disabled", Toast.LENGTH_SHORT).show();
        }

        if (BluetoothPrintManager.isBluetoothAvaliable()) {
            bluetoothSession = new BluetoothPrinterDiscoverySession(this);
            bluetoothSession.setObserver(generalObserver);
        } else {
            Toast.makeText(this, "Bluetooth not avaliable", Toast.LENGTH_SHORT).show();
        }

        serialSession = new SerialPrinterDiscoverySession();
        serialSession.setObserver(generalObserver);

    }

    private PrinterDiscoveryObserver generalObserver = new PrinterDiscoveryObserver() {

        @Override
        public void onPrinterAdded(final PrinterInfo printer) {
            runOnUiThread(new Runnable() {
                public void run() {
                    printers.add(printer);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDiscoveryFailed() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    contentView.button1.setText("search failed");
                }
            });
        }

        @Override
        public void onDiscoveryCompleted() {
            if (isSearching()) {
                return;
            }
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    contentView.button1.setText("search completed");
                }
            });
        }

        @Override
        public void onDiscoveryCancelled() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    contentView.button1.setText("search cancelled");
                }
            });
        }

        @Override
        public void onDiscoveryStarted() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    contentView.button1.setText("searching");
                }
            });
        }
    };

    private void clearSearchResult() {
        printers.clear();
        adapter.notifyDataSetChanged();
    }

    private boolean isSearching() {
        boolean netSearching = netSession != null && netSession.isSearching();
        boolean usbSearching = usbSession != null && usbSession.isSearching();
        boolean bluetoothSearching = bluetoothSession != null && bluetoothSession.isSearching();
        return netSearching || usbSearching || bluetoothSearching;
    }

    private class SimplePrinterAdapter extends BaseAdapter {
        private ArrayList<PrinterInfo> mPrinters;
        private Context mContext;
        private LayoutInflater inflater;

        public SimplePrinterAdapter(Context context, ArrayList<PrinterInfo> printers) {
            mContext = context;
            mPrinters = printers;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mPrinters.size();
        }

        @Override
        public PrinterInfo getItem(int position) {
            return mPrinters.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListItem item;
            PrinterInfo printer = getItem(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_layout_printer, null);
                item = new ListItem(convertView);
                convertView.setTag(item);
            } else {
                item = (ListItem) convertView.getTag();
            }

            item.setPrinter(printer);
            return convertView;
        }

        private class ListItem {
            public TextView tvId;
            public TextView tvName;
            public TextView tvType;
            public TextView tvStatus;

            public ListItem(View view) {
                tvId = (TextView) view.findViewById(R.id.tv_id);
                tvName = (TextView) view.findViewById(R.id.tv_name);
                tvType = (TextView) view.findViewById(R.id.tv_type);
                tvStatus = (TextView) view.findViewById(R.id.tv_status);
            }

            public void setPrinter(PrinterInfo printer) {
                tvId.setText(printer.getIdentifier());
                tvName.setText(printer.getName());
                tvStatus.setText(printer.isConnected() ? "connected" : "disconnected");
                tvType.setText(printer.getTypeName());
            }

        }


    }

}
