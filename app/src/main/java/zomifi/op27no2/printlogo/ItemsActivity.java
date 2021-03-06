package zomifi.op27no2.printlogo;

import android.accounts.Account;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ResultStatus;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.customer.CustomerConnector;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.clover.sdk.v3.employees.EmployeeConnector;
import com.clover.sdk.v3.inventory.InventoryConnector;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static android.R.attr.start;
import static zomifi.op27no2.printlogo.R.id.tabbutton;

public class ItemsActivity extends Activity implements View.OnClickListener, CustomPriceEnteredListener {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor edt;
    private MerchantConnector merchantConnector;
    private EmployeeConnector mEmployeeConnector;
    private OrderConnector orderConnector;
    private CustomerConnector customerConnector;
    private InventoryConnector inventoryConnector;
    private Context mContext;
    private Account account;
    private Order mOrder;
    private Handler handler = new Handler();
    private Runnable runnable;

    private static String  mercID ="default";
    private static String  mEmail = "do data";
    private static String  mAdd= "no data";
    private static String  mCity = "no data";
    private static String  mState= "no data";
    private static String  mCountry= "no data";
    private static String  mNumber= "no data";
    private static String  mZip= "no data";
    private static String  mTimezone= "no data";
    private static String  mName= "no data";


    private LinearLayout multiplesLayout;
    private LinearLayout itemsLayout;
    private String PRICE_STRING = "";
    private String mOrderId;
    private Long mTimestamp;
    private Long mTotal;
    private Map<String, OrderItem> mItems;

    private ProgressDialog progressDialog;
    private long donationAmount = 0;
    private String donationName = "";
    private int TRIGGER = 0;
    private int RESULT_CODE = 2;
    private int mMultiplicity = 1;

    private static final int PAY_REQUEST_CODE = 1;
    private static final int ORDER_REQUEST_CODE = 3;
    private int mOrderMode = 1;
    private Boolean LOCKED = false;
    private Boolean TOUCH_ACTIVE = false;
    private Boolean customItemPresent = false;

    private ArrayList<Boolean> addList = new ArrayList<Boolean>(Arrays.asList(false,false,false,false,false,false,false,false));
    private boolean isOpened = false;
    private boolean building = false;
    private boolean navigateAway = false;
    private static final NumberFormat mCurrencyFormat = DecimalFormat.getCurrencyInstance(Locale.US);
    private ArrayList<ArrayList<Integer>> lineItems = new ArrayList<ArrayList<Integer>>();
    private ArrayList<Integer> ipeNums = new ArrayList<Integer>();

    private Button logoutButton;
    private Button saveButton;
    private Button orderButton;
    private Button settingsButton;
    private Button tabButton;

    Button myButtons[] = new Button[9];
    Button multipleButtons[] = new Button[20];

    private ImageButton bRight;
    private ImageButton bLeft;
    private int cPage = 1;

    private TextView pageText;
    private TextView tabText;
    private TextView multipleText;
    private TextView titleText;
    private ValueAnimator colorAnimation;
    private ScrollView multiplesScroll;
    private FirebaseHelper mHelper;

    ArrayList<String> mOrderIds = new ArrayList<String>();
    ArrayList<Long> mOrderTotals = new ArrayList<Long>();
    ArrayList<Long> mOrderTimestamps = new ArrayList<Long>();
    ArrayList<Map<String, OrderItem>> mOrderItems = new ArrayList<Map<String, OrderItem>>();

    //IPE Functions
    private RelativeLayout ipeLayout;
    private GridView gridView;
    private Boolean editPrimed = false;
    private Boolean removePrimed = false;
    private Boolean ipeScreen = true;
    private Button editButton;
    private Button syncButton;
    private Button removeButton;
    Map<String,String> mIPEs = new HashMap<String, String>();
    private List<String> gridData;
    private GridViewAdapter customGridViewAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(zomifi.op27no2.printlogo.R.layout.activity_items);

        //THIS IS THE ONLY SCREEN NOW

        sharedPreferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        edt = sharedPreferences.edit();
        sharedPreferences.getInt("mode",1);
        mercID = sharedPreferences.getString("mercID", "");
        mContext = this;
        mHelper = new FirebaseHelper(this);
        mHelper.initialize();
        donationAmount = sharedPreferences.getLong("customLongPrice", 0l);
        donationName = sharedPreferences.getString("customName", "");

        multiplesLayout = (LinearLayout) findViewById(zomifi.op27no2.printlogo.R.id.multiplicity);
        itemsLayout = (LinearLayout) findViewById(zomifi.op27no2.printlogo.R.id.items_view);
        ipeLayout = (RelativeLayout) findViewById(zomifi.op27no2.printlogo.R.id.ipe_layout);
        orderButton = (Button) findViewById(zomifi.op27no2.printlogo.R.id.orderbutton);
        saveButton = (Button) findViewById(zomifi.op27no2.printlogo.R.id.save_button);
        removeButton = (Button) findViewById(R.id.remove_button);
        editButton = (Button) findViewById(R.id.edit_button);
        syncButton = (Button) findViewById(R.id.sync_button);

        tabButton = (Button) findViewById(tabbutton);
        multipleText = (TextView) findViewById(R.id.numbertext);
        pageText = (TextView) findViewById(R.id.pages);
        titleText = (TextView) findViewById(R.id.title_text);
        multiplesScroll = (ScrollView) findViewById(R.id.multiplesscroll);


        int colorFrom = ContextCompat.getColor(mContext, R.color.White);
        int colorTo = ContextCompat.getColor(mContext, R.color.Red);
        colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(250); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                tabText.setTextColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimation.setRepeatCount(Animation.INFINITE);

        if(sharedPreferences.getBoolean("showMultiple", true)){
            multiplesLayout.setVisibility(View.VISIBLE);
        }
        else{
            multiplesLayout.setVisibility(View.GONE);
        }

        bRight = (ImageButton) findViewById(zomifi.op27no2.printlogo.R.id.bright);
        bLeft = (ImageButton) findViewById(zomifi.op27no2.printlogo.R.id.bleft);

        for(int i=0; i<9; i++) {
            int resID = getResources().getIdentifier("button"+(i+1), "id", "zomifi.op27no2.printlogo");
            myButtons[i] = ((Button) findViewById(resID));
            myButtons[i].setOnClickListener(this);
        }
        for(int i=0; i<20; i++) {
            int resID = getResources().getIdentifier("mult"+(i+1), "id", "zomifi.op27no2.printlogo");
            multipleButtons[i] = ((Button) findViewById(resID));
            multipleButtons[i].setOnClickListener(this);
        }


        Button homeButton = (Button) findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(ItemsActivity.this, NavigationActivity.class);
                ItemsActivity.this.startActivity(myIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                resetMultiplicity();
                createOrders();

            }
        });
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* navigateAway = true;
                Intent intent1 = new Intent("com.clover.intent.action.START_ORDERS");
                startActivity(intent1);*/
                CustomIPEOrderListDialog customDialog = new CustomIPEOrderListDialog(mContext);
                customDialog.setCancelable(false);
                customDialog.show();
            }
        });

        tabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateAway = true;
/*                Intent myIntent = new Intent(ItemsActivity.this, IPEActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                ItemsActivity.this.startActivity(myIntent);*/
                toggleScreen();
            }
        });
        findViewById(R.id.textclear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "IPEs and Items Deselected", Toast.LENGTH_SHORT).show();
                edt.putBoolean("isactive", false);
                edt.commit();
                //updateText();
                resetScreens();

            }
        });

        cPage = sharedPreferences.getInt("currentPage",1);
        bLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cPage > 1) {
                    cPage = cPage - 1;
                    edt.putInt("currentPage", cPage);
                    edt.commit();
                    setUpButtonNames(cPage);
                    setUpCurrentItems(cPage);
                }

            }
        });
        bRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cPage < 10) {
                    cPage = cPage + 1;
                    edt.putInt("currentPage", cPage);
                    edt.commit();
                    setUpButtonNames(cPage);
                    setUpCurrentItems(cPage);
                }
            }
        });
        setUpButtonNames(cPage);

        mMultiplicity = sharedPreferences.getInt("multiplicity",1);
        setButtonColor(mMultiplicity);
        this.PRICE_STRING = "";

        //IPE functions
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRemove();
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEdit();
            }
        });
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupGrid();
            }
        });



        gridView = (GridView) findViewById(R.id.tabList);
        gridData = new ArrayList<>();
        for(int i=0; i<49 ; i++){
            gridData.add("Click to Add");
        }

        customGridViewAdapter = new GridViewAdapter(this, gridData);
        gridView.setAdapter(customGridViewAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String uniqueID = sharedPreferences.getString("uniqueID" + position, "");
                // if position is: not full, show dialog
                if (sharedPreferences.getBoolean("full" + position, false) == false) {
                    CustomIPEListDialog customDialog = new CustomIPEListDialog(mContext, position);
                    customDialog.setCancelable(false);
                    customDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            //onResume();
                            setupGrid();
                        }
                    });
                    customDialog.show();

                //else position is full and can be edited, removed, or highlighted/unhighlighted
                }else if(editPrimed ==true){
                    //EDIT TAB
                    toggleEdit();
                    //pass "" order ID since we are querying for most recent.
                    CustomIPEOrderItemsListDialog customDialog = new CustomIPEOrderItemsListDialog(mContext, gridData.get(position), sharedPreferences.getString("uniqueID"+position,""), "", true);
                    customDialog.setCancelable(false);
                    customDialog.show();

                }else if(removePrimed ==true){
                    //REMOVE TAB
                    toggleRemove();
                    String name = gridData.get(position);
                    Boolean ipeClocked = sharedPreferences.getBoolean(mIPEs.get(name) + "clocked", false);
                    if(ipeClocked){
                        Toast.makeText(mContext,"Must Be Clocked Out Before Removal", Toast.LENGTH_LONG).show();
                    }else {
                        IPESelector mSelector = new IPESelector(mContext);
                        mSelector.clearPosition(position, uniqueID);
                        //onResume();
                        setupGrid();
                    }

                }
                //else if position is: active, turn black - inactive, turn red
                else{
                    if (sharedPreferences.getBoolean("active" + position, false) == false) {
                        edt.putBoolean("active"+position, true);
                        edt.putBoolean(uniqueID+"active", true);

                    }else{
                        edt.putBoolean("active"+position, false);
                        edt.putBoolean(uniqueID+"active", false);
                    }
                    edt.commit();
                    setupGrid();
                    //TODO changed to setup grid make sure thats ok...
                    /*updateIPENumbs();
                    customGridViewAdapter.notifyDataSetChanged();*/
                }

            }
        });

        getEmployees();


    }

    private void updateText(){
        int tabposition = sharedPreferences.getInt("currenttab",0);
        if(sharedPreferences.getBoolean("isactive", false) == true && !sharedPreferences.getString("name"+tabposition, "Click to Add").equals("Click to Add")) {
            tabText.setText(sharedPreferences.getString("name" + tabposition, "Error"));


            colorAnimation.start();

        }
        else{
            tabText.setText("None");
            if(colorAnimation.isRunning()) {
                colorAnimation.end();
            }
            tabText.setTextColor(ContextCompat.getColor(mContext, R.color.White));
        }


    }

    private void logout() {
        finishAffinity();
        mEmployeeConnector.logout(new EmployeeConnector.EmployeeCallback<Void>() {
            @Override
            public void onServiceSuccess(Void result, ResultStatus status) {
                super.onServiceSuccess(result, status);
                System.out.println("logged out");
            }

            @Override
            public void onServiceFailure(ResultStatus status) {
                super.onServiceFailure(status);
                System.out.println("logout failure");
            }

            @Override
            public void onServiceConnectionFailure() {
                super.onServiceConnectionFailure();
                System.out.println("connection failure");
            }
        });
        disconnect();

    }

    private void setUpButtonNames(int currentPage) {
        pageText.setText("Page " + currentPage);
        sharedPreferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
     /*   for (int i = 0; i < 8; i++) {
            myButtons[i].setText(sharedPreferences.getString(1 + "_" + currentPage + "button" + (i + 1) + "_name", "Add from Settings") + "\n" + sharedPreferences.getString(1 + "_" + currentPage + "button" + (i + 1) + "_price", ""));
        }*/
        for(int i=0;i<8;i++){
            String id = sharedPreferences.getString(1+"_"+currentPage+"button"+(i+1)+"_id", "");
            myButtons[i].setText(sharedPreferences.getString(id+"_name", "Add from Settings") + "\n" + formatPrice(sharedPreferences.getString(id + "_price" + 1, "")));
        }

        /*try {
                DB snappydb = DBFactory.open(mContext); //create or open an existing database using the default name
                for(int i=0;i<8;i++) {
                    String id = snappydb.get(1+"_"+currentPage+"button"+(i+1)+"_id");
                    if (snappydb.exists(id + "_name") && snappydb.exists(id + "_price" + 1)) {
                        myButtons[i].setText(snappydb.get(id + "_name") + "\n" + formatPrice(sharedPreferences.getString(id + "_price" + 1, "")));

                    } else {
                        myButtons[i].setText("Add from Settings");
                    }
                }
                snappydb.close();

        } catch (SnappydbException e) {
            System.out.println("snappy error: " + e.getMessage());
        }*/



    }

    private void setUpCurrentItems(int currentPage) {
        sharedPreferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE);

        for(int i=0;i<9;i++){
            String id = sharedPreferences.getString(1+"_"+currentPage+"button"+(i+1)+"_id", "");
            myButtons[i].setText(sharedPreferences.getString(id+"_name", "Add from Settings") + "\n" + formatPrice(sharedPreferences.getString(id + "_price" + 1, "")));
   //         myButtons[i].setText(sharedPreferences.getString(1+"_"+currentPage + "button" + (i + 1) + "_name", "Add from Settings") + "\n" + /*formatPrice(*/sharedPreferences.getString(1+"_"+currentPage + "button" + (i + 1) + "_price", ""))/*)*/;
            myButtons[i].setBackgroundResource(zomifi.op27no2.printlogo.R.drawable.aqua_green_button);

            for(int j=0;j<lineItems.size();j++){
                if(lineItems.get(j).get(1) == (i+1) && lineItems.get(j).get(0) == cPage){
                    int mult = lineItems.get(j).get(2);
                    myButtons[i].setBackgroundResource(zomifi.op27no2.printlogo.R.drawable.red_button);
                    myButtons[i].setText(sharedPreferences.getString(id + "_name", "Add from Settings") + " x" + lineItems.get(j).get(2) + "\n" + formatPrice(sharedPreferences.getString(id + "_price" + 1, "")));
          //          myButtons[i].setText(sharedPreferences.getString(1 + "_" + currentPage + "button" + (i + 1) + "_name", "Add from Settings") + " x" + lineItems.get(j).get(2) + "\n" + /*formatPrice(*/sharedPreferences.getString(1 + "_" + currentPage + "button" + (i + 1) + "_price", ""))/*)*/;
                    break;
                }
            }

            ((Button) findViewById(myButtons[8].getId())).setText("Custom Amount");
        }
    }

    @NonNull
    private String formatPrice(String PRICE_STRING) {

        SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString("currencyCode", "USD");
        mCurrencyFormat.setCurrency(Currency.getInstance(currency));
        String price = "";
        if(!PRICE_STRING.equals("")) {
            long value = Long.valueOf(PRICE_STRING);
            price = mCurrencyFormat.format(value / 100.0);
        }
        return price;

    }

    @Override
    protected void onResume() {
        super.onResume();
        prefsToLineItems();

        building = false;
        resetMultiplicity();
        edt.putInt("multiplicity", 1);
        edt.commit();
        // updateText();

        for(int i=0;i<8;i++){
            addList.set(i,false);
        }

        setUpButtonNames(cPage);
        setUpCurrentItems(cPage);
       // mMultiplicity = sharedPreferences.getInt("multiplicity",1);
        mOrderMode = sharedPreferences.getInt("ordermode",1);
        setButtonColor(mMultiplicity);


        if(sharedPreferences.getBoolean("showMultiple", true)){
            multiplesLayout.setVisibility(View.VISIBLE);
        }
        else{
            multiplesLayout.setVisibility(View.GONE);
        }




        System.out.println("OnResume Called, locked: " + LOCKED);
        LOCKED = true;
        navigateAway = false;

        // hideSystemUI();
        // UiChangerListener();
    //    setupFullscreenMode();

        // Retrieve the Clover account
        if (account == null) {
            account = CloverAccount.getAccount(this);

            if (account == null) {
                Toast.makeText(this, getString(zomifi.op27no2.printlogo.R.string.no_account), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }


        // Create and Connect
        connect();

    }

    @Override
    protected void onPause() {

        lineItemsToPrefs();
        disconnect();
        System.out.println("OnPause Called" + LOCKED);
       /* if (LOCKED) {
            Intent i = new Intent(this, CustomerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(i);
        }*/


        super.onPause();

    }

    @Override
    protected void onDestroy() {
        System.out.println("Main onDestroy called");

        super.onDestroy();

    }

    private void connect() {
        disconnect();
        if (account != null) {
            merchantConnector = new MerchantConnector(this, account, null);
            merchantConnector.connect();

            mEmployeeConnector = new EmployeeConnector(this, account, null);
            mEmployeeConnector.connect();

            customerConnector = new CustomerConnector(this, account, null);
            customerConnector.connect();

            orderConnector = new OrderConnector(this, account, null);
            orderConnector.connect();

            if(inventoryConnector!=null) {
                inventoryConnector.disconnect();
            }
            inventoryConnector = new InventoryConnector(this, account, null);
            inventoryConnector.connect();

        }
    }

    private void disconnect() {

        if (mEmployeeConnector != null) {
            mEmployeeConnector.disconnect();
            mEmployeeConnector = null;
        }
        if (merchantConnector != null) {
            merchantConnector.disconnect();
            merchantConnector = null;
        }
        if (customerConnector != null) {
            customerConnector.disconnect();
            customerConnector = null;
        }
        if (orderConnector != null) {
            orderConnector.disconnect();
            orderConnector = null;
        }
        if(inventoryConnector!=null) {
            inventoryConnector.disconnect();
            inventoryConnector = null;
        }
    }

    private void setButtonColor(int num){
        for(int i=0;i<20;i++){
            if(num-1 == i) {
                ((Button) findViewById(multipleButtons[i].getId())).setBackgroundResource(zomifi.op27no2.printlogo.R.drawable.red_button);
            }
            else{
                ((Button) findViewById(multipleButtons[i].getId())).setBackgroundResource(zomifi.op27no2.printlogo.R.drawable.aqua_green_button);
            }
        }

    }


    @Override
    public void onClick(View v)
    {
        for(int i=0; i<20; i++){
            if(v.getId() == multipleButtons[i].getId()){
                mMultiplicity = i+1;
                setButtonColor(i+1);
                edt.putInt("multiplicity", i+1);
                edt.commit();
            }
            else if(i<9 && v.getId() == myButtons[i].getId() ){
                if(!myButtons[i].getText().equals("Add from Settings"+"\n"+"")) {
                    addLineItems(i+1);
                }
            }

        }
        if(v.getId() == zomifi.op27no2.printlogo.R.id.button9)
        {
            LineItemPriceSetter lineItemPriceSetter = new LineItemPriceSetter(this,1,false);
            lineItemPriceSetter.setCustomPriceEnteredListener(this);
            lineItemPriceSetter.setFromItemsActivity(true);
            lineItemPriceSetter.show();
        }



    }

    @Override
    public void setPrice(String orderID, String customName, int mode, long price, Boolean isPayment)
    {
        System.out.println("setPrice listener" + price);
        System.out.println("setPrice name" + customName);
        customItemPresent = true;
        donationAmount = price;
        donationName = customName;
        this.PRICE_STRING = String.valueOf(donationAmount);
        titleText.setText("Custom Amount: " + formatPrice(PRICE_STRING));
        edt.putLong("customLongPrice", donationAmount);
        edt.putString("customName", customName);
        edt.putBoolean("customPresent", true);
        edt.commit();
    }

    @Override
    public void changeButton(int buttonID, String PRICE_STRING)
    {
      /*  if(buttonID == 9){
            System.out.println("test listener" + PRICE_STRING);
            donationAmount = Long.parseLong(PRICE_STRING);
        }*/
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PAY_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                //Once the secure payment activity completes the result and its extras can be worked with

              //  Payment payment = data.getParcelableExtra(Intents.EXTRA_PAYMENT);
               // String amountString = String.format("%.2f", ((Double) (0.01 * payment.getAmount())));
              //  Toast.makeText(getApplicationContext(), getString(R.string.payment_successful, amountString), Toast.LENGTH_SHORT).show();


            } else {
                Toast.makeText(getApplicationContext(), getString(zomifi.op27no2.printlogo.R.string.payment_failed), Toast.LENGTH_SHORT).show();

            }

        }
        if (requestCode == RESULT_CODE/* && resultCode == RESULT_OK*/) {
            System.out.println("test1" + data);
         //   b1.setText(sharedPreferences.getString("recentItemId","error"));

        } else {
            System.out.println("test2");
        }
        if (requestCode == ORDER_REQUEST_CODE/* && resultCode == RESULT_OK*/) {
            System.out.println("test1" + data);

        } else {
            System.out.println("test2");
        }
    }

    private void receiptDialog(final String orderId)
    {
        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Thank you for your donation! Would you like a receipt?").setCancelable(
                false).setPositiveButton("Print or Email Receipt",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        System.out.println("receipt test: " + LOCKED + " " + mOrder);
                        Intent intent1 = new Intent(Intents.ACTION_START_PRINT_RECEIPTS);
                        intent1.putExtra(Intents.EXTRA_ORDER_ID, orderId);
                        startActivity(intent1);

                        /*PrintJob pj = new StaticBillPrintJob.Builder().order(mOrder).build();
                        pj.print(CustomerActivity.this, account);*/
/*
                    }
                }).setNegativeButton("Decline",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                    }
                });
        AlertDialog alert = builder.create()//.........................…;
        alert.show();

        alert.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        alert.show();
                //Set the dialog to immersive
        alert.getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());
                //Clear the not focusable flag from the window
                        alert.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);*/
        LOCKED = false;
        CustomDialog customDialog = new CustomDialog(mContext);
        customDialog.setHeaderText("Donate Here");
        customDialog.setBodyText("Thank you for your donation!\n\nWould you like a receipt?");
        customDialog.setCancelable(false);
        customDialog.setOrderId(orderId);
        customDialog.setDialogFrom(CustomDialog.RECEIPT_ACTIVITY);
        System.out.println("Dialog called, locked: "+LOCKED);
        customDialog.show();
    }


    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
        //    setFullscreen();
        }
    }


    public void payIntent(){
        Intent intent = null;
        if(mOrderMode == 1) {
            navigateAway = true;
            intent = new Intent(Intents.ACTION_CLOVER_PAY);
            /*if(sharedPreferences.getBoolean("isactive",false)){
                intent.putExtra(Intents.EXTRA_ORDER_ID, sharedPreferences.getString("orderid"+sharedPreferences.getInt("currenttab",0), ""));
            }
            else {*/
                intent.putExtra(Intents.EXTRA_ORDER_ID, mOrderId);
            //}
            startActivityForResult(intent, PAY_REQUEST_CODE);
        }
        else if(mOrderMode == 2) {
            navigateAway = true;
            intent = new Intent(Intents.ACTION_START_ORDER_MANAGE);
            /*if(sharedPreferences.getBoolean("isactive",false)){
                intent.putExtra(Intents.EXTRA_ORDER_ID, sharedPreferences.getString("orderid"+sharedPreferences.getInt("currenttab",0), ""));
            }
            else {*/
                intent.putExtra(Intents.EXTRA_ORDER_ID, mOrderId);
           // }
            startActivityForResult(intent, ORDER_REQUEST_CODE);
        }


    }

    private void createOrders()
    {
        System.out.println("create orders for IPE nums: "+ipeNums);

        ArrayList<String> orderIdList = new ArrayList<String>();
        ArrayList<String> ipeIDList = new ArrayList<String>();
        ArrayList<String> ipeNameList = new ArrayList<String>();
        //ipeNums contains the integers of active ipes
        for(int i=0; i<ipeNums.size(); i++) {
            System.out.println("ipe iterate"+i);
            if (sharedPreferences.getBoolean("active" + ipeNums.get(i), false) == true) {
                System.out.println("ipe iterate isactive" + i);
                String myOrderId = sharedPreferences.getString("orderId" + i, "");
                orderIdList.add(myOrderId);
                ipeIDList.add(sharedPreferences.getString("uniqueID" + ipeNums.get(i), ""));
                ipeNameList.add(sharedPreferences.getString("name" + ipeNums.get(i), ""));
            }
        }
        new OrderAsyncTask(orderIdList, ipeIDList, ipeNameList).execute();

    }

    private class OrderAsyncTask extends AsyncTask<Void, Void, ArrayList<Order>> {
        ArrayList<String> orderIdList;
        ArrayList<String> ipeIDList;
        ArrayList<String> ipeNameList;

        public OrderAsyncTask(ArrayList<String> orderIdList, ArrayList<String> ipeIDList, ArrayList<String> ipeNameList){
            super();
            this.orderIdList = orderIdList;
            this.ipeIDList = ipeIDList;
            this.ipeNameList = ipeNameList;
        }

        @Override
        protected final ArrayList<Order> doInBackground(Void... params) {
            ArrayList<Order> myOrders = new ArrayList<Order>();
            mOrderTotals.clear();
            mOrderTimestamps.clear();
            mOrderItems.clear();

            try {
                for(int i=0; i<orderIdList.size();i++) {

                    Order myOrder = null;
                    if (orderIdList.get(i).equals("")) {
                        // order doesn't exist, create it
                        System.out.println("order doesn't exist, create it" + i);
                        myOrder = orderConnector.createOrder(new Order());
                    } else {
                        // otherwise, retrieve it
                        System.out.println("otherwise, retrieve it" + i);
                        myOrder = orderConnector.getOrder(orderIdList.get(i));
                    }
                    myOrder= orderConnector.updateOrder(myOrder);

                    Long iTotal = 0l;
                    Long time = Calendar.getInstance().getTimeInMillis();
                    Map<String, OrderItem> iOrderItems = new HashMap<String, OrderItem>();
                    int counter = 0;

                    //add all line items
                    for (int j = 0; j < lineItems.size(); j++) {
                       // System.out.println("Line items called:");
                        if (lineItems.get(j).get(1) == 9) {
                          //  System.out.println("Line items2  called:" +donationAmount);
                            //add custom amount
                            // LineItem myLineItem = new LineItem();
                            //myLineItem.setPrice(donationAmount);
                            OrderItem mOrderItem = new OrderItem(donationName, "Custom", donationAmount, time ,0l,0l, false, false, false,false);
                            iOrderItems.put("item"+counter, mOrderItem);
                            iTotal = iTotal+donationAmount;
                            counter++;
                            // orderConnector.addCustomLineItem(myOrder.getId(), myLineItem, false);

                        } else {
                         //   System.out.println("Line items 3 called:");
                            for (int k = 0; k < lineItems.get(j).get(2); k++) {
                                //items stored on buttons by id (mode_page_position, page and position are first two in array for line items, third is # of copies of that item)), then look up price/name of item based on id

                           //     System.out.println("Line items 4 called:");
                                String id = sharedPreferences.getString(1 + "_" + lineItems.get(j).get(0) + "button" + lineItems.get(j).get(1) + "_id", "error");
                                String name = sharedPreferences.getString(id + "_name", "");
                                String category = sharedPreferences.getString(id + "_category", "");
                                Long price = Long.parseLong(sharedPreferences.getString(id + "_price" + 1, "0"));
                                /*LineItem myLineItem = new LineItem();
                                myLineItem.setName(name);
                                myLineItem.setPrice(price);*/
                                OrderItem mOrderItem = new OrderItem(name, category, price, time ,0l,0l, false, false, false, false);
                                iOrderItems.put("item"+counter, mOrderItem);
                                iTotal = iTotal+price;
                                counter++;
                                // orderConnector.addCustomLineItem(myOrder.getId(), myLineItem, false);
                                //orderConnector.addFixedPriceLineItem(myOrder.getId(), sharedPreferences.getString(1 + "_" + lineItems.get(j).get(0) + "button" + lineItems.get(j).get(1) + "_id", "error"), null, null);
                            }
                        }
                    }
                    myOrder= orderConnector.updateOrder(myOrder);
                    myOrders.add(myOrder);


                    mOrderTotals.add(iTotal);
                    mOrderTimestamps.add(time);
                    mOrderItems.add(iOrderItems);


                }
            } catch (RemoteException e) {
                System.out.println("Order  ERROR1:"+e.getMessage());

            } catch (ClientException e) {
                System.out.println("Order  ERROR2:"+e.getMessage());

            } catch (ServiceException e) {
                System.out.println("Order  ERROR3:"+e.getMessage());

            } catch (BindingException e) {
                System.out.println("Order  ERROR4:"+e.getMessage());

            }

            return myOrders;
        }

        @Override
        protected final void onPostExecute(ArrayList<Order> orders) {
            // Populate the UI
            System.out.println("order: " + orders);

            String toastString = "";
     /*       ArrayList<String> mOrderIds = new ArrayList<String>();
            ArrayList<Long> mOrderTotals = new ArrayList<Long>();
            ArrayList<Long> mOrderTimestamps = new ArrayList<Long>();
            ArrayList<Map<String, OrderItem>> mOrderItems = new ArrayList<Map<String, OrderItem>>();
*/

            for(int i=0;i<orders.size();i++) {
                toastString = toastString + orders.get(i).getId()+", ";
                mOrderIds.add(orders.get(i).getId());
                // mOrderTotals.add(orders.get(i).getTotal());
               //  mOrderTimestamps.add(orders.get(i).getCreatedTime());
  /*              List<LineItem> myItems = orders.get(i).getLineItems();
                Map<String, OrderItem> items = new HashMap<String, OrderItem>();
                for (int j = 0; j < myItems.size(); j++) {
                    String name = myItems.get(j).getName();
                    Long price = myItems.get(j).getPrice();
                    OrderItem mOrderItem = new OrderItem(name, price, orders.get(i).getCreatedTime(), 0l, false);
                    items.put("item" + j, mOrderItem);
                }
                mOrderItems.add(items);*/
            }

            Toast.makeText(getApplicationContext(), "Orders Updated:"+toastString, Toast.LENGTH_SHORT).show();
            System.out.println("order results: " + mOrderTotals);
            System.out.println("order results: " + mOrderTimestamps);
            System.out.println("order results: " + mOrderItems);
            System.out.println("order results: " + mOrderIds);
            System.out.println("order results: " + ipeIDList);
            mHelper.createOrUpdateIPEOrders("IPEOrders", mOrderTotals, mOrderTimestamps, mOrderItems, mOrderIds, ipeIDList, ipeNameList);
            resetScreens();
        }
    }

    private void resetMultiplicity()
    {
        Runnable mrunnable = new Runnable() {
            @Override
            public void run() {
                mMultiplicity = 1;
                multiplesScroll.scrollTo(0,0);
                setButtonColor(mMultiplicity);
            }
        };

        handler.postDelayed(mrunnable, 50);
    }

    public void addLineItems(int pos){
        // items stored (page 1-x, button number 1-9, multiplicity)
        Boolean newitem = true;

        for(int j=0;j<lineItems.size();j++) {
            if (pos!=9 && lineItems.get(j).get(1) == (pos) && lineItems.get(j).get(0) == cPage) {
                lineItems.remove(j);
                newitem = false;
            }
        }
        if(newitem) {
            // IF ITEM ISN'T ADDED
            ArrayList<Integer> singleitem = new ArrayList<Integer>();
            singleitem.add(cPage);
            singleitem.add(pos);
            singleitem.add(mMultiplicity);
            lineItems.add(singleitem);
        }
        System.out.println("Line Items"+lineItems);

        resetMultiplicity();
        setUpCurrentItems(cPage);
    }


    private void lineItemsToPrefs(){
        edt.putInt("item_size", lineItems.size());
        for(int i=0; i<lineItems.size(); i++){
            edt.putInt(i+"item_page", lineItems.get(i).get(0));
            edt.putInt(i+"item_pos", lineItems.get(i).get(1));
            edt.putInt(i + "item_mult", lineItems.get(i).get(2));
        }
        edt.commit();
    }

    private void prefsToLineItems(){
        lineItems.clear();
        ipeNums.clear();
        for(int i=0; i<sharedPreferences.getInt("item_size", 0); i++) {
            ArrayList<Integer> singleitem = new ArrayList<Integer>();
            singleitem.add(sharedPreferences.getInt(i + "item_page", 0));
            singleitem.add(sharedPreferences.getInt(i + "item_pos", 0));
            singleitem.add(sharedPreferences.getInt(i + "item_mult", 0));
            lineItems.add(singleitem);
        }
        for(int j=0; j<49; j++) {
            if (sharedPreferences.getBoolean("active" + j, false) == true){
                ipeNums.add(j);
                System.out.println("ipeNums: "+ipeNums);
            }
        }
    }

    private void resetScreens(){

        for(int k=0; k<49; k++){
            edt.putBoolean("active" + k, false);
        }
        for (String name : mIPEs.keySet()) {
            edt.putBoolean(mIPEs.get(name) + "active", false);
        }
        edt.commit();
        lineItems.clear();
        setUpCurrentItems(cPage);
        setUpButtonNames(cPage);
        titleText.setText("");
        setupGrid();

    }



    //IPE methods

    private void toggleRemove(){
        if (removePrimed) {
            removePrimed = false;
            removeButton.setAlpha(1f);
        } else {
            removePrimed = true;
            removeButton.setAlpha(0.5f);
            Toast.makeText(getApplicationContext(), "Select Tab to Remove", Toast.LENGTH_SHORT).show();
        }
    }
    private void toggleEdit(){
        if (editPrimed) {
            editPrimed = false;
            editButton.setAlpha(1f);
        } else {
            editPrimed = true;
            editButton.setAlpha(0.5f);
            Toast.makeText(getApplicationContext(), "Select Tab to Edit", Toast.LENGTH_SHORT).show();
        }
    }

    public void setupGrid() {
        updateIPENumbs();
        System.out.println("setup grid called");
        gridData.clear();
        final IPESelector mSelector = new IPESelector(mContext);
        final Set<String> mSet = new HashSet<String>();
        final int[] i = {0};

        //If active add name to grid
        for (String name : mIPEs.keySet()) {
            if (sharedPreferences.getBoolean(mIPEs.get(name) + "active", false) == true) {
                gridData.add(name);
                    System.out.println("active" + name);
            }
        }
        Collections.sort(gridData, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        //if active make name red with showEmployee, store active w/ #
        for(int k=0;k<gridData.size();k++){
            System.out.println("show red ");
            String name = gridData.get(k);
            mSelector.showEmployee(name, mIPEs.get(name), k);
            edt.putBoolean("active" + k, true);
            edt.commit();
            mSet.add(name);
        }

        //automatically add any who are clocked but were not added manually
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference thisRef = database.getReference().child(mercID);
        final ArrayList<String> mClocked = new ArrayList<String>();
        Query newquery = thisRef.child("ClockedList").orderByKey();
        newquery.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //    System.out.println("filter datasnap" + dataSnapshot);
                        for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                            String mName = (String) mSnapshot.getValue();
                            mClocked.add(mName);
                        }
                        System.out.println("clocked list: "+mClocked);

                        // check if each clocked employee is added already, if not add to data and set of names
                        for (int j=0; j<mClocked.size(); j++) {
                            String name = mClocked.get(j);
                            System.out.println("clocked name: "+name);

                            if (!mSet.contains(name)){
                                System.out.println("add name: "+name);

                                gridData.add(name);
                                mSelector.addEmployee(name, mIPEs.get(name), (gridData.size() - 1));
                                mSet.add(name);
                            }
                        }

                        //fill remaining slots with blank 'add to grid'
                        int fill = gridData.size();
                        if(fill<49){
                            for(int j=fill; j<49;j++){
                                gridData.add("Click to Add");
                                mSelector.clearGrid(j);
                                //         System.out.println("click to add: " + j);
                                i[0]++;
                            }
                        }

                        updateIPENumbs();
                        customGridViewAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("error looking for employee clocked" + databaseError.toString());
                        logError("retrieving employee clocked list error:" + databaseError.toString());

                        //fill remaining slots with blank 'add to grid'
                        int fill = gridData.size();
                        if(fill<49){
                            for(int j=fill; j<49;j++){
                                gridData.add("Click to Add");
                                mSelector.clearGrid(j);
                                //         System.out.println("click to add: " + j);
                                i[0]++;
                            }
                        }

                        updateIPENumbs();
                        customGridViewAdapter.notifyDataSetChanged();

                    }
                });


        //if added, add to grid, but not necessarily active
        for (String name : mIPEs.keySet()) {
            if (sharedPreferences.getBoolean(mIPEs.get(name) + "added", false) == true) {
                if (!mSet.contains(name)){
                    gridData.add(name);
                    mSelector.addEmployee(name, mIPEs.get(name), (gridData.size() - 1));
                    mSet.add(name);
                              //System.out.println("grid filling added" + gridData);
                }
            }
        }
/*
        //fill remaining slots with blank 'add to grid'
        int fill = gridData.size();
        if(fill<49){
            for(int j=fill; j<49;j++){
                gridData.add("Click to Add");
                mSelector.clearGrid(j);
                //         System.out.println("click to add: " + j);
                i++;
            }
        }

        updateIPENumbs();
        customGridViewAdapter.notifyDataSetChanged();*/
    }

    private void updateIPENumbs(){
        ipeNums.clear();
        for(int j=0; j<49; j++) {
            if (sharedPreferences.getBoolean("active" + j, false) == true){
                ipeNums.add(j);
                //  System.out.println("ipeNums: "+ipeNums);
            }
        }
    }

    private void getEmployees() {
        final Long finish = System.currentTimeMillis();
        System.out.println("timestamp database: "+(finish-start));

        final Map<String,String> mEmployees = new HashMap<String, String>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference thisRef = database.getReference().child(mercID);
        DatabaseReference myRef = thisRef.child("EmployeeNames");
        myRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot dataSnapshot) {
                        //    System.out.println("filter datasnap" + dataSnapshot);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO figure out if finishsetup can be put in handler to help
                                for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                                    EmployeeLight mEmployee = mSnapshot.getValue(EmployeeLight.class);
                                    mEmployees.put(mEmployee.gesStageName(), mEmployee.gesUniqueID());
                                }
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        finishSetup(mEmployees);
                                    }
                                });
                            }
                        }).start();

                        Long finish2 = System.currentTimeMillis();
                       // System.out.println("timestamp firebase: "+(finish2-finish)+" milliseconds");

                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                      //  System.out.println("error looking for employee names" + databaseError.toString());
                    }
                });
    }
    public void finishSetup(Map<String,String> data){
        mIPEs = data;
        System.out.println("my employees: "+mIPEs);
        setupGrid();
    }

    //Switch between UIs
    public void toggleScreen(){
        if(ipeScreen == true){
            //SET TO ITEMS SCREEN
            ipeLayout.setVisibility(View.GONE);
            editButton.setVisibility(View.GONE);
            removeButton.setVisibility(View.GONE);
            itemsLayout.setVisibility(View.VISIBLE);
            multiplesLayout.setVisibility(View.VISIBLE);
            multipleText.setVisibility(View.VISIBLE);
            tabButton.setText("Select IPE");
            ipeScreen=false;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) orderButton.getLayoutParams();
            params.addRule(RelativeLayout.LEFT_OF, tabButton.getId());
            params.removeRule(RelativeLayout.RIGHT_OF);
            orderButton.setLayoutParams(params);
        }else{
            //SET TO IPE SCREEN
            ipeLayout.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            removeButton.setVisibility(View.VISIBLE);
            itemsLayout.setVisibility(View.GONE);
            multiplesLayout.setVisibility(View.GONE);
            multipleText.setVisibility(View.GONE);
            tabButton.setText("Select Items");
            ipeScreen=true;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) orderButton.getLayoutParams();
            params.addRule(RelativeLayout.RIGHT_OF, saveButton.getId());
            params.removeRule(RelativeLayout.LEFT_OF);
            orderButton.setLayoutParams(params);
        }
    }

    public void logError(String error){

        Crashlytics.logException(new Exception(error));
        CustomErrorDialog customDialog = new CustomErrorDialog(error,mContext);
        customDialog.setCancelable(false);
        customDialog.show();

        Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();

    }


}