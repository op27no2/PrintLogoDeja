package zomifi.op27no2.printlogo;

import android.accounts.Account;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IInterface;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.printer.CashDrawer;
import com.clover.sdk.v1.printer.ReceiptRegistrationConnector;
import com.clover.sdk.v1.printer.job.PrintJob;
import com.clover.sdk.v1.printer.job.StaticBillPrintJob;
import com.clover.sdk.v3.order.LineItem;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Andrew on 3/4/2016.
 * This code is written for Scrap Workouts LLC.
 */
public class CustomIPEOrderItemsListDialog extends Dialog implements View.OnClickListener, CustomPriceEnteredListener, CustomDiscountListener , CustomManagerListener
{
    private SharedPreferences prefs;
    private SharedPreferences.Editor edt;
    private Account account;
    private Order mOrder;
    private OrderConnector orderConnector;
    private String orderID;
    private static final NumberFormat mCurrencyFormat = DecimalFormat.getCurrencyInstance(Locale.US);
    private ReceiptRegistrationConnector receiptConnector;
    private Handler handler = new Handler();

    private static Context context;
    private static LinearLayout bottomButtons;
    private String  orderId = "";

    private TextView wordText;

    private static Button submitButton;
    private static Button payButton;
    private static Button backButton;
    private static Button cancelButton;

    private Boolean isEditing;

    private static FirebaseHelper mHelper;
    private DatabaseReference myRef;
    private ValueEventListener myEmployeeListener;
    private ChildEventListener mChildEventListener;
    private ChildEventListener mChildEventListener2;

    EditText edittexts[] = new EditText[8];
    private Spinner mStateSpinner;
    private Spinner mStatusSpinner;
    private Boolean isNew;
    private static String employeeUniqueID;
    private static String employeeName;
    private static String orderUniqueID;
    private static FirebaseRecyclerAdapter mAdapter;
    private DatabaseReference myItemsRef;
    private Query mOrderQuery;
    private Query mOrderQuery2;
    private static String lastClickedShift;
    private static String lastClickedPosition;
    private String mercID;
    private static LinearLayoutManager mManager;
    private DatabaseReference thisRef;

    private TextView orderCreated;
    private TextView orderTotal;
    private Long oTotal;
    private TextView orderBalance;
    private Long oBalance;
    private String oIPE;
    private Boolean oVoided;
    private TextView orderIPE;
    private TextView orderStatus;
    private TextView orderVoided;
    private TextView shiftTotal;

    private static LinearLayout voidLayout;
    private static LinearLayout voidOrderLayout;
    private static LinearLayout closeOrderLayout;
    private static LinearLayout payOrderLayout;

    private Button cancelItemButton;
    private static Button voidItemButton;

    private Button cancelVoidButton;
    private Button voidOrderButton;
    private Button showOrderVoidButton;
    private Button clocknavButton;
    private Button printButton;

    private Button cancelCloseButton;
    private Button closeOrderButton;
    private Button showOrderCloseButton;
    Button priceButtons[] = new Button[5];
    private Long mPayment;
    private Long mTotal = 0l;
    private Long totalDuration = 0l;
    private Boolean fromIPEScreen;
    private static CustomDiscountListener mListener;
    private Boolean loaded;
    CustomManagerListener mManagerListener;

    public CustomIPEOrderItemsListDialog(Context context, String employeeName, String employeeUniqueID, String orderUniqueID, Boolean fromIPEScreen)
    {
        //if coming from order list, pass an order push ID to get a specific order
        //otherwise, pass orderUniqueID as null to load the most recent order (e.g. from 'view tab')
        super(context);
        this.context = context;
        this.employeeName = employeeName;
        this.employeeUniqueID = employeeUniqueID;
        this.orderUniqueID = orderUniqueID;
        this.fromIPEScreen = fromIPEScreen;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_ipeitemlist);
        prefs = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        edt = prefs.edit();
        loaded = false;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mercID = prefs.getString("mercID", "");
        thisRef = database.getReference().child(mercID);
        totalDuration = 0l;
        mListener = (CustomDiscountListener) this;
        mManagerListener = this;

        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mHelper = new FirebaseHelper(context);
        mHelper.initialize();

        // Retrieve the Clover account
        if (account == null) {
            account = CloverAccount.getAccount(context);
        }
        orderCreated = (TextView) findViewById(R.id.order_created);
        orderTotal = (TextView) findViewById(R.id.order_total);
        orderBalance = (TextView) findViewById(R.id.order_balance);
        orderIPE = (TextView) findViewById(R.id.order_ipe);
        orderStatus = (TextView) findViewById(R.id.order_status);
        orderVoided = (TextView) findViewById(R.id.order_void);
        shiftTotal = (TextView) findViewById(R.id.shift_total);
        bottomButtons = (LinearLayout) findViewById(R.id.bottombuttons);

        showOrderVoidButton = (Button) findViewById(R.id.void_button);
        showOrderVoidButton.setOnClickListener(this);
        showOrderCloseButton = (Button) findViewById(R.id.close_button);
        showOrderCloseButton.setOnClickListener(this);

        closeOrderLayout = (LinearLayout) findViewById(R.id.close_order_buttons);
        closeOrderButton = (Button) findViewById(R.id.close_order_confirm);
        cancelCloseButton = (Button) findViewById(R.id.close_order_cancel);

        voidLayout = (LinearLayout) findViewById(R.id.void_buttons);
        voidItemButton = (Button) findViewById(R.id.void_item_confirm);
        cancelItemButton = (Button) findViewById(R.id.void_item_cancel);

        voidOrderLayout = (LinearLayout) findViewById(R.id.void_order_buttons);
        voidOrderButton = (Button) findViewById(R.id.void_order_confirm);
        cancelVoidButton = (Button) findViewById(R.id.void_order_cancel);

        payOrderLayout = (LinearLayout) findViewById(R.id.cash_buttons);
        payButton = (Button) findViewById(R.id.payment_button);
        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            showPayment();
            CashDrawer.open(context, account);

            }
        });

        submitButton = (Button) findViewById(R.id.customDialogSubmitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeTextListeners();
                if(mAdapter !=null) {
                    mAdapter.cleanup();
                }
                final RecyclerView recycler = (RecyclerView) findViewById(R.id.shift_recycler);
                recycler.setAdapter(null);
                dismiss();
            }
        });

        printButton = (Button) findViewById(R.id.print_button);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loaded) {

                    ArrayList<OrderItem> mItems = new ArrayList<OrderItem>();
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        OrderItem mOrderItem = (OrderItem) mAdapter.getItem(i);
                        mTotal = mTotal + mOrderItem.gesPrice();
                        mItems.add(mOrderItem);
                    }


                    registerReceiptRegistration();
                    PrintBuilder mBuilder = new PrintBuilder();
                    mBuilder.initialize(context, 1);
                    mBuilder.PrintOrderItemsReceipt(mItems, oBalance, oTotal, oVoided, oIPE, formatTime(totalDuration));


                    Order order = null;
                    try {
                        order = new PrintAsync().execute().get();
                        PrintJob pj = new StaticBillPrintJob.Builder().order(order).build();
                        pj.print(CustomIPEOrderItemsListDialog.context, account);
                        delayUnregister();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            }
        });


        cancelItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideVoid();
            }
        });


/*        showOrderVoidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrderVoid();
            }
        });*/
        voidOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHelper.voidIPEOrder(orderUniqueID);
                mHelper.closeIPEOrder(orderUniqueID);
                hideOrderVoid();
            }
        });
        cancelVoidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOrderVoid();
            }
        });


/*        showOrderCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOrderClose();
            }
        });*/
        closeOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(oBalance > 0){
                    CustomManagerDialog managerDialog = new CustomManagerDialog(context, false);
                    managerDialog.setCustomManagerListener(mManagerListener);
                    managerDialog.setButtonID(closeOrderButton.getId());
                    managerDialog.show();
                }
                else{
                    mHelper.closeIPEOrder(orderUniqueID);
                    DatabaseReference eRef = thisRef.child("Employees").child(employeeUniqueID);
                    Long time = Calendar.getInstance().getTimeInMillis();
                    mHelper.clockOut(eRef, employeeName, employeeUniqueID, time);
                    hideOrderClose();
                }
            }
        });
        cancelCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOrderClose();
            }
        });

        for(int i=0; i<5; i++) {
            int resID = context.getResources().getIdentifier("money"+(i+1), "id", "zomifi.op27no2.printlogo");
            priceButtons[i] = ((Button) findViewById(resID));
            priceButtons[i].setOnClickListener(this);
        }




        DisplayMetrics metrics = this.context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        getWindow().setLayout((6 * width) / 7, (8 * height) / 9);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if(!orderUniqueID.equals("")) {
            mHelper.updateOrderTotal(thisRef.child("IPEOrders").child(orderUniqueID));
            setupTextListeners();
        }
        setupClockListeners();


        Query recentOrderQuery = null;
        Query preQuery = null;

        //coming from IPE activity screen
        if(orderUniqueID.equals("")) {

            preQuery = thisRef.child("Employees").child(employeeUniqueID).child("orders").limitToLast(1);
            preQuery.addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                orderUniqueID = dataSnapshot.getChildren().iterator().next().getKey();
                                System.out.println("order ref: " + orderUniqueID);

                                DatabaseReference myRef = thisRef.child("IPEOrders");
                                Query newquery = myRef.orderByKey().equalTo(orderUniqueID);
                                System.out.println("order query: " + newquery.getRef() + "  orderID: " + orderUniqueID);

                                loadItemsQuery(newquery);
                                mHelper.updateOrderTotal(thisRef.child("IPEOrders").child(orderUniqueID));
                                setupTextListeners();
                            }
                            else{
                                //paying will mess things up if no order exists yet
                                payButton.setOnClickListener(null);
                                showOrderCloseButton.setOnClickListener(null);
                                showOrderVoidButton.setOnClickListener(null);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("no orders found for employee:" + employeeUniqueID);
                        }
                    });

        }
        else{
            //coming from dialog of all orders
            DatabaseReference myRef = thisRef.child("IPEOrders");
            recentOrderQuery = myRef.orderByKey().equalTo(orderUniqueID);
            System.out.println("order query: " + recentOrderQuery.getRef() + "  orderID: " + orderUniqueID);

            loadItemsQuery(recentOrderQuery);
        }

        connect();
    //end oncreate
    }

    private void connect(){
        receiptConnector = new ReceiptRegistrationConnector(context, account, new ServiceConnector.OnServiceConnectedListener() {
            @Override
            public void onServiceConnected(ServiceConnector<? extends IInterface> serviceConnector) {
                System.out.println("receipt service connected");
                unregisterReceiptRegistration();
                edt.putBoolean("can_print", false);
                edt.commit();
            }

            @Override
            public void onServiceDisconnected(ServiceConnector<? extends IInterface> serviceConnector) {
                unregisterReceiptRegistration();
            }
        });
        receiptConnector.connect();
        orderConnector = new OrderConnector(context, account, new ServiceConnector.OnServiceConnectedListener() {
            @Override
            public void onServiceConnected(ServiceConnector<? extends IInterface> serviceConnector) {

            }

            @Override
            public void onServiceDisconnected(ServiceConnector<? extends IInterface> serviceConnector) {

            }
        });
        orderConnector.connect();
    }

    private void loadItemsQuery(Query recentOrderQuery){
        final DatabaseReference[] myOrderRef = {null};

        recentOrderQuery.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        myOrderRef[0] = dataSnapshot.getChildren().iterator().next().getRef();
                        System.out.println("order ref: " + myOrderRef[0]);

                        myItemsRef = myOrderRef[0].child("items");


                        loadList();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("no orders found for employee:" + employeeUniqueID);
                    }
                });
    }

    private void loadList(){

        final RecyclerView recycler = (RecyclerView) findViewById(R.id.shift_recycler);
        recycler.setHasFixedSize(true);
        mManager = new LinearLayoutManager(context);
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        recycler.setLayoutManager(mManager);


        mAdapter = new FirebaseRecyclerAdapter<OrderItem, ItemHolder>(OrderItem.class, R.layout.list_item_ipeorderitem, ItemHolder.class, myItemsRef) {
            @Override
            public void populateViewHolder(ItemHolder itemViewHolder, OrderItem mOrderItem, int position) {
                SimpleDateFormat df = new SimpleDateFormat("MM/dd   hh:mm:ss a");
                String time = df.format(mOrderItem.gesTimestamp());
                itemViewHolder.setText1(mOrderItem.gesName());
                itemViewHolder.setText2(formatPrice(mOrderItem.gesPrice()));
                itemViewHolder.setText3(time);
                if(mOrderItem.gesHasDiscount() !=null && mOrderItem.gesHasDiscount()){
                    itemViewHolder.setTextDiscount(formatPrice(mOrderItem.gesDiscount()));
                }
                if(mOrderItem.gesVoided()){
                    itemViewHolder.setText4("VOID");
                }
                else{
                    itemViewHolder.setText4("");
                }

                if(position % 2 == 0){
                    itemViewHolder.setBackgroundDark();
                }
                else{
                    itemViewHolder.setBackgroundLight();
                }
            }
        };
        recycler.setAdapter(mAdapter);

    }


    @Override
    public void changeButton(int buttonID, String PRICE_STRING) {

    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.close_button){
           /* CustomManagerDialog managerDialog = new CustomManagerDialog(context, false);
            managerDialog.setCustomManagerListener(this);
            managerDialog.setButtonID(R.id.close_button);
            managerDialog.show();*/
            showOrderClose();

        }
        if(view.getId() == R.id.void_button){
            CustomManagerDialog managerDialog = new CustomManagerDialog(context, false);
            managerDialog.setCustomManagerListener(this);
            managerDialog.setButtonID(R.id.void_button);
            managerDialog.show();
        }
        for(int i=0; i<5; i++){
            if(view.getId() == priceButtons[i].getId()){
                //get amount from prefs - or set custom amount
                mPayment = null;
                if(i==4){
                    // custom price dialog for payment
                    LineItemPriceSetter lineItemPriceSetter = new LineItemPriceSetter(context ,1, true);
                    lineItemPriceSetter.setCustomPriceEnteredListener(this);
                    lineItemPriceSetter.setButtonIndex(6);
                    lineItemPriceSetter.show();
                }
                else {
                    switch (i) {
                        case 0:
                            mPayment = prefs.getLong("cash_amount" + 1, -1000);
                            break;
                        case 1:
                            mPayment = prefs.getLong("cash_amount" + 2, -2000);
                            break;
                        case 2:
                            mPayment = prefs.getLong("cash_amount" + 3, -5000);
                            break;
                        case 3:
                            mPayment = prefs.getLong("cash_amount" + 4, -10000);
                            break;
                    }

                    mHelper.addIPEPaymentItem(orderUniqueID, mPayment);
                    mHelper.updateOrderTotal(thisRef.child("IPEOrders").child(orderUniqueID));
                    hidePayment();
                }

            }
        }
    }

    @Override
    public void setDiscount(long discount, DatabaseReference itemRef, long price, long pDiscount) {
        mHelper.setIPELineItemDiscount(discount, itemRef, price, pDiscount);
    }


    public static class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CustomManagerListener{
        View mView;
        public TextView tv1;
        public TextView tvd;
        public TextView tv2;
        public TextView tv3;
        public TextView tv4;
        public Button btn1;
        OrderItem mOrderItem;
        DatabaseReference myItemRef;

        public ItemHolder(View itemView) {
            super(itemView);
            mView = itemView;
         //   myButton = (Button) itemView.findViewById(R.id.button1);
         //   myButton.setOnClickListener(this);
            tv1 = (TextView) itemView.findViewById(R.id.text1);
            tv1.setOnClickListener(this);
            tvd = (TextView) itemView.findViewById(R.id.textDiscount);
            tv2 = (TextView) itemView.findViewById(R.id.text2);
            tv2.setOnClickListener(this);
            tv3 = (TextView) itemView.findViewById(R.id.text3);
            tv3.setOnClickListener(this);
            tv4 = (TextView) itemView.findViewById(R.id.text4);
            tv4.setOnClickListener(this);
            btn1 = (Button) itemView.findViewById(R.id.void_item_listbutton);
            btn1.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        public void setText1(String name) {
            TextView field = (TextView) mView.findViewById(R.id.text1);
            field.setText(name);
        }
        public void setTextDiscount(String text) {
            TextView field = (TextView) mView.findViewById(R.id.textDiscount);
            field.setText(text);
        }
        public void setText2(String text) {
            TextView field = (TextView) mView.findViewById(R.id.text2);
            field.setText(text);
        }
        public void setText3(String text) {
            TextView field = (TextView) mView.findViewById(R.id.text3);
            field.setText(text);
        }
        public void setText4(String text) {
            TextView field = (TextView) mView.findViewById(R.id.text4);
            field.setText(text);
        }

        public void setBackgroundDark(){
            LinearLayout layout = (LinearLayout) mView.findViewById(R.id.lines);
            layout.setBackgroundResource(R.drawable.lightgray_button);
        }
        public void setBackgroundLight(){
            LinearLayout layout = (LinearLayout) mView.findViewById(R.id.lines);
            layout.setBackgroundResource(R.drawable.whitegray_button);
        }

        @Override
        public void onClick(View v) {
            System.out.println("onclick");

            int position = getAdapterPosition();
            mOrderItem = (OrderItem) mAdapter.getItem(position);
            myItemRef = mAdapter.getRef(position);
            Long price = mOrderItem.gesPrice();
            Long pDiscount = mOrderItem.gesDiscount();

            if(v.getId() == btn1.getId()){
                CustomManagerDialog managerDialog = new CustomManagerDialog(context, false);
                managerDialog.setCustomManagerListener(this);
                managerDialog.setButtonID(btn1.getId());
                managerDialog.show();

            }else {
                System.out.println("clicked");
                DiscountPriceSetter discountPriceSetter = new DiscountPriceSetter(context, myItemRef, price, pDiscount);
                discountPriceSetter.setCustomDiscountListener(mListener);
                discountPriceSetter.show();
            }

        }

        @Override
        public void managerCallback(int resourceID) {
            showVoid(myItemRef);
        }
    }

    private static void showVoid(final DatabaseReference itemRef){
        voidLayout.setVisibility(View.VISIBLE);
        bottomButtons.setVisibility(View.GONE);
        voidItemButton.setOnClickListener(null);
        voidItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // void clicked item update order total
                mHelper.voidIPEOrderItem(itemRef);

                hideVoid();
                System.out.println("will void item");
            }
        });
    }

    private static void hideVoid(){
        voidLayout.setVisibility(View.GONE);
        bottomButtons.setVisibility(View.VISIBLE);
    }

    private void showOrderVoid(){
        voidOrderLayout.setVisibility(View.VISIBLE);
        bottomButtons.setVisibility(View.GONE);
    }
    private void hideOrderVoid(){
        voidOrderLayout.setVisibility(View.GONE);
        bottomButtons.setVisibility(View.VISIBLE);
    }

    private void showOrderClose(){
        closeOrderLayout.setVisibility(View.VISIBLE);
        bottomButtons.setVisibility(View.GONE);
    }
    private void hideOrderClose(){
        closeOrderLayout.setVisibility(View.GONE);
        bottomButtons.setVisibility(View.VISIBLE);
    }

    private void showPayment(){
        payOrderLayout.setVisibility(View.VISIBLE);
    }
    private void hidePayment(){
        payOrderLayout.setVisibility(View.GONE);
    }


    @NonNull
    private String formatPrice(Long value) {
        //test format code
        SharedPreferences sharedPreferences = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        //default USD from prefs
        String currency = sharedPreferences.getString("currencyCode", "USD");
        mCurrencyFormat.setCurrency(Currency.getInstance(currency));
        String price = mCurrencyFormat.format(value / 100.0);
        return price;

    }

    private void setupTextListeners(){
       mOrderQuery = thisRef.child("IPEOrders").orderByKey().equalTo(orderUniqueID);
       mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                System.out.println("datasnapp:"+dataSnapshot);
                // A new comment has been added, add it to the displayed list
               // IPEOrder mOrder = dataSnapshot.getChildren().iterator().next().getValue(IPEOrder.class);
                IPEOrder mOrder = dataSnapshot.getValue(IPEOrder.class);
                SimpleDateFormat df = new SimpleDateFormat("MM/dd     hh:mm:ss a");
                String time = df.format(mOrder.gesTimestamp());
                orderCreated.setText(time);
                orderTotal.setText(formatPrice(mOrder.gesTotal()));
                oTotal = mOrder.gesTotal();
                oVoided = mOrder.gesVoided();
                if(mOrder.gesBalance() != null) {
                    orderBalance.setText(formatPrice(mOrder.gesBalance()));
                    oBalance = mOrder.gesBalance();
                }
                orderIPE.setText(mOrder.gesEmployeeName());
                oIPE = mOrder.gesEmployeeName();
                if(mOrder.gesVoided()) {
                    orderVoided.setText("Void");
                    RecyclerView recycler = (RecyclerView) findViewById(R.id.shift_recycler);
                    recycler.setVisibility(View.GONE);
                    TextView mText = (TextView) findViewById(R.id.items_void);
                    mText.setVisibility(View.VISIBLE);
                }
                else{
                    orderVoided.setText("");
                }
                if(mOrder.gesOpen()) {
                    orderStatus.setText("Open");
                }
                else{
                    orderStatus.setText("Closed");
                    RecyclerView recycler = (RecyclerView) findViewById(R.id.shift_recycler);
                    if(fromIPEScreen) {
                        recycler.setVisibility(View.GONE);
                        orderBalance.setText("");
                        orderCreated.setText("");
                        orderTotal.setText("");
                        orderVoided.setText("");
                        orderStatus.setText("");
                        TextView mText = (TextView) findViewById(R.id.items_void);
                        mText.setVisibility(View.GONE);
                    }
                }
                loaded = true;


                // ...
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so displayed the changed comment.
                IPEOrder mOrder = dataSnapshot.getValue(IPEOrder.class);
                //    IPEOrder mOrder = dataSnapshot.getValue(IPEOrder.class);
                SimpleDateFormat df = new SimpleDateFormat("MM/dd     hh:mm:ss a");
                String time = df.format(mOrder.gesTimestamp());
                orderCreated.setText(time);
                orderTotal.setText(formatPrice(mOrder.gesTotal()));
                oTotal = mOrder.gesTotal();
                oVoided = mOrder.gesVoided();
                if(mOrder.gesBalance() != null) {
                    orderBalance.setText(formatPrice(mOrder.gesBalance()));
                    oBalance = mOrder.gesBalance();
                }
                orderIPE.setText(mOrder.gesEmployeeName());
                oIPE = mOrder.gesEmployeeName();
                if(mOrder.gesOpen()) {
                    orderStatus.setText("Open");
                }
                else{
                    orderStatus.setText("Closed");
                }
                if(mOrder.gesVoided()) {
                    orderVoided.setText("Void");
                    RecyclerView recycler = (RecyclerView) findViewById(R.id.shift_recycler);
                    recycler.setVisibility(View.GONE);
                    TextView mText = (TextView) findViewById(R.id.items_void);
                    mText.setVisibility(View.VISIBLE);
                }
                else{
                    orderVoided.setText("");
                }
                loaded = true;

                // ...
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("postComments:onCancelled: "+ databaseError.toException());
                Toast.makeText(context, "Failed to load order.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mOrderQuery.addChildEventListener(mChildEventListener);



    }

    private void setupClockListeners() {

        mOrderQuery2 = thisRef.child("Employees").orderByKey().equalTo(employeeUniqueID);
        System.out.println("query employee:" + mOrderQuery2);
        mChildEventListener2 = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // A new comment has been added, add it to the displayed list
                // IPEOrder mOrder = dataSnapshot.getChildren().iterator().next().getValue(IPEOrder.class);
                final Employee mEmployee = (Employee) dataSnapshot.getValue(Employee.class);
                final String myID = mEmployee.gesUniqueID();
                final Long time = Calendar.getInstance().getTimeInMillis();
                final DatabaseReference employeeRef = dataSnapshot.getRef();

                clocknavButton = (Button) findViewById(R.id.clocknav_button);
                if(mEmployee.gesClocked() == true) {
                    clocknavButton.setText("Clock Out");
                }
                else if(mEmployee.gesClocked() == false){
                    clocknavButton.setText("Clock In");
                }

                clocknavButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mEmployee.gesClocked() == true) {
                            //clock employee out
                            Long clicktime = Calendar.getInstance().getTimeInMillis();
                            mHelper.clockOut(employeeRef,mEmployee.gesName(), myID, clicktime);
                        }
                        else if(mEmployee.gesClocked() == false){
                            //clock employee in
                            Long clicktime = Calendar.getInstance().getTimeInMillis();
                            mHelper.clockIn(employeeRef, mEmployee.gesName(), myID, clicktime);
                        }


                    }
                });

                //get most recent instance of 10am in millis
                Long now = System.currentTimeMillis();
                Long duration = 0l;
                Long tenam;

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 10);
                if(now < calendar.getTimeInMillis()){
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    tenam = calendar.getTimeInMillis();
                }
                else{
                    tenam = calendar.getTimeInMillis();
                }


                Map<String, Shift> mShifts = mEmployee.gesShifts();
                if(mShifts !=null) {
                    for (Shift shift : mShifts.values()) {
                        shift.gesClockIn();
                        if (shift.gesClockIn() > tenam) {
                            if (shift.gesClockOut() != null && shift.gesClockOut() != 0) {

                                System.out.println("clockin:" + formatDate(shift.gesClockIn()) + " clockout:" + formatDate(shift.gesClockOut()) + "duration: " + formatTime(shift.gesClockOut() - shift.gesClockIn()));
                                duration = duration + (shift.gesClockOut() - shift.gesClockIn());
                            } else {
                                System.out.println("clockin:" + shift.gesClockIn() + " now:" + now);
                                duration = duration + (now - shift.gesClockIn());
                            }
                        }
                    }
                }

                totalDuration = duration;
                shiftTotal.setText(formatTime(totalDuration));
                System.out.println("total duration:"+totalDuration);


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {

                final Employee mEmployee = (Employee) dataSnapshot.getValue(Employee.class);
                final String myID = mEmployee.gesUniqueID();
                final Long time = Calendar.getInstance().getTimeInMillis();
                final DatabaseReference employeeRef = dataSnapshot.getRef();

                clocknavButton = (Button) findViewById(R.id.clocknav_button);
                if(mEmployee.gesClocked() == true) {
                    clocknavButton.setText("Clock Out");
                }
                else if(mEmployee.gesClocked() == false){
                    clocknavButton.setText("Clock In");
                }


                clocknavButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(mEmployee.gesClocked() == true) {
                            //clock employee out
                            Long clicktime = Calendar.getInstance().getTimeInMillis();
                            mHelper.clockOut(employeeRef,mEmployee.gesName(), myID, clicktime);
                        }
                        else if(mEmployee.gesClocked() == false){
                            //clock employee in
                            Long clicktime = Calendar.getInstance().getTimeInMillis();
                            mHelper.clockIn(employeeRef, mEmployee.gesName(), myID, clicktime);
                        }


                    }
                });

                //get most recent instance of 10am in millis
                Long now = System.currentTimeMillis();
                Long duration = 0l;
                Long tenam;

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 10);
                if(now < calendar.getTimeInMillis()){
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    tenam = calendar.getTimeInMillis();
                }
                else{
                    tenam = calendar.getTimeInMillis();
                }


                Map<String, Shift> mShifts = mEmployee.gesShifts();
                if(mShifts !=null) {
                    for (Shift shift : mShifts.values()) {
                        shift.gesClockIn();
                        if (shift.gesClockIn() > tenam) {
                            if (shift.gesClockOut() != null && shift.gesClockOut() != 0) {
                                System.out.println("clockin:" + formatDate(shift.gesClockIn()) + " clockout:" + formatDate(shift.gesClockOut()) + "duration: " + formatTime(shift.gesClockOut() - shift.gesClockIn()));
                                duration = duration + (shift.gesClockOut() - shift.gesClockIn());
                            } else {
                                //  System.out.println("clockin:"+shift.gesClockIn() +" now:" + now + " duration: "+formatTime(now - shift.gesClockIn()));
                                // duration = duration + (now - shift.gesClockIn());
                            }
                        }
                    }
                }
                totalDuration = duration;
                shiftTotal.setText(formatTime(totalDuration));
                System.out.println("total duration:"+formatTime(totalDuration));

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("listener error :onCancelled: "+ databaseError.toException());
                Toast.makeText(context, "Failed to load order.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mOrderQuery2.addChildEventListener(mChildEventListener2);


    }

        private void removeTextListeners(){
        if(mChildEventListener != null) {
            mOrderQuery.removeEventListener(mChildEventListener);
        }
        if(mChildEventListener2 != null) {
            mOrderQuery2.removeEventListener(mChildEventListener2);
        }
    }

    @Override
    public void setPrice(String orderID,String name, int mode, long price, Boolean isPayment)
    {
        mPayment = -price;
        mHelper.addIPEPaymentItem(orderUniqueID, mPayment);
        mHelper.updateOrderTotal(thisRef.child("IPEOrders").child(orderUniqueID));
        hidePayment();
    }


    private void registerReceiptRegistration() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Create and Connect
                receiptConnector.register(Uri.parse(ReceiptRegistrationProviderDejaVu.CONTENT_URI_IMAGE.toString()), new ReceiptRegistrationConnector.ReceiptRegistrationCallback<Void>());

                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                System.out.println("REGISTERED!!");

            }
        }.execute();
    }

    private void unregisterReceiptRegistration() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                receiptConnector.unregister(Uri.parse(ReceiptRegistrationProviderDejaVu.CONTENT_URI_IMAGE.toString()), new ReceiptRegistrationConnector.ReceiptRegistrationCallback<Void>());

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                System.out.println("UNREGISTERED");

                // shouldn't need to disconnect here - will disconnect when activity is exited regardless, and connect call disconnects before reconnecting
                // disconnect();
            }
        }.execute();
    }

    private void delayUnregister()
    {
        Runnable mrunnable = new Runnable() {
            @Override
            public void run() {
                unregisterReceiptRegistration();
            }
        };

        handler.postDelayed(mrunnable, 5000);
    }



    private class PrintAsync extends AsyncTask<Void, Void, Order> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected final Order doInBackground(Void... params) {


            Order tOrder = null;
            try {
                //creating order just for timestamp and dummy order for receipt printing, no actual line items
                tOrder = orderConnector.createOrder(new Order());

                LineItem myLineItem = new LineItem();
                myLineItem.setName("Total");
                myLineItem.setPrice(oTotal);
                orderConnector.addCustomLineItem(tOrder.getId(), myLineItem, false);
                tOrder = orderConnector.updateOrder(tOrder);


            } catch (RemoteException e) {
                System.out.println("Order CREATTTED ERROR"+e.getMessage());
                e.printStackTrace();
            } catch (ClientException e) {
                System.out.println("Order CREATTTED ERROR"+e.getMessage());
                e.printStackTrace();
            } catch (ServiceException e) {
                System.out.println("Order CREATTTED ERROR"+e.getMessage());
                e.printStackTrace();
            } catch (BindingException e) {
                System.out.println("Order CREATTTED ERROR"+e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Order created" + tOrder);
            return tOrder;
        }

        @Override
        protected final void onPostExecute(Order order) {



        }
    }


    @Override
    public void managerCallback(int resourceID) {
        /*if(resourceID == R.id.close_button) {
            showOrderClose();
        }*/
        if(resourceID == R.id.void_button) {
            showOrderVoid();
        }
        if(resourceID == closeOrderButton.getId() ) {
            mHelper.closeIPEOrder(orderUniqueID);
            DatabaseReference eRef = thisRef.child("Employees").child(employeeUniqueID);
            Long time = Calendar.getInstance().getTimeInMillis();
            mHelper.clockOut(eRef, employeeName, employeeUniqueID,time);
            hideOrderClose();

        }
    }


    private String formatTimee(Long time){
        return (new SimpleDateFormat("HH:mm:ss")).format(new Date(time));
    }

    public String formatTime(Long milliseconds) {
        long seconds = (long) Math.floor(milliseconds /1000);

        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h, m, s);
    }
    public String formatDate(Long milliseconds) {
        Date date = new Date(milliseconds); //Input your time in milliseconds
        String mDate = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a").format(date);
        return mDate;
    }
}
