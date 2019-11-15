package de.schildbach.tdcwallet.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tdcoinj.core.Transaction;

import java.io.IOException;
import java.io.InputStream;

import de.schildbach.tdcwallet.Configuration;
import de.schildbach.tdcwallet.Constants;
import de.schildbach.tdcwallet.R;
import de.schildbach.tdcwallet.WalletApplication;

public class VerifyDocumentActivity extends AbstractWalletActivity{
    private AbstractWalletActivity activity;
    private WalletApplication application;
    private Configuration config;
    public static final String INTENT_EXTRA_KEY = "vtx";
    private WebView tv;
    private String html;
    private final Handler handler = new Handler();
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    String trHex = "";
    Transaction trans;
/*&    public static void start(final Context context) {
        context.startActivity(new Intent(context, VerifyDocumentActivity.class));
    }

    public static void start(final Context context, final String trHex) {
        final Intent intent = new Intent(context, VerifyDocumentActivity.class);
        intent.putExtra(INTENT_EXTRA_KEY, trHex);
        context.startActivity(intent);
    }
*/
public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
    }
    return data;
}
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_verify_activity);
        //String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        log.warn("???");
        //log.warn("intent URI", savedInstanceState.toUri(0));
        //BlockchainService.start(this, false);
        tv = new WebView(this);
        tv.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                log.info("WEB_VIEW", "error code:" + errorCode);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        tv.getSettings().setLoadsImagesAutomatically(true);
        tv.getSettings().setAllowFileAccess(true);
        tv.getSettings().setAllowContentAccess(true);
        tv.getSettings().setAllowFileAccessFromFileURLs(true);
        tv.getSettings().setAllowUniversalAccessFromFileURLs(true);
        tv.getSettings().setAppCacheEnabled(false);

        //tv.setText("Extras: \n\r");
        setContentView(tv);
        backgroundThread = new HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        //String vtx = "";
        //StringBuilder str = new StringBuilder();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String vUri = bundle.getString("result");
            if (vUri.startsWith("tdcoin-verify://")) {
                trHex = vUri.substring(16);
            }
            //getIntent().putExtra(INTENT_EXTRA_KEY, vtx);
            requestTxInfo();
        /*    Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key = it.next();
                str.append(key);
                str.append(":");
                str.append(bundle.get(key));
                str.append("\n\r");
            }*/
            //tv.setText("txid:\n"+trHex);
        }
    }
    private String doctype(Integer dtype){
     String ret = "";
        switch (dtype){
            case 0: ret = "Certificate";
                break;
            case 1: ret = "Pasport";
                break;
        }
        return ret;
    }

    private String images2Html(JSONArray json){
        String ret = "";
        if(json.length()>0){
            for (int i=0; i < json.length(); i++) {
                try {
                    ret = ret + "<img src=\""+ json.getString(i)+"\">";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return ret;
        } else return "";
    }

    private Boolean is_verified(JSONObject jobj,String orgname,String addr){
        boolean ret = false;

        return ret;
    }

    private String json2html(JSONObject json, String tdcaddr ) throws JSONException {
        String html = "";
        JSONObject verified_issuers;
        String jsonVI = loadStringFromAsset("verified-issuers.json");
        /*Address tmp;
        try {
            tmp = Address.fromString(Constants.NETWORK_PARAMETERS, tdcaddr);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }*/
        //if(tdcaddr.equals("aA22Zs5xWdmaSZnMngPHZ4ubqpzwx1FwFA\n")) log.info("addresses equal: "+tdcaddr);
        try {
            verified_issuers = new JSONObject( jsonVI );
            //log.info("verified issuers: "+verified_issuers.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            verified_issuers = null;
        }
        //log.info("tx addr: "+tdcaddr);
        //log.info("issuer name: "+json.getJSONObject("ORG_ISSUER").getString("NAME"));
        //log.info("issuer name2: "+verified_issuers.get( tmp.toString() ));
        try {
            String nameFromVerifiedIssuers = verified_issuers.getJSONObject( tdcaddr ).getString("NAME");
            String nameFromQrImage = json.getJSONObject("ORG_ISSUER").getString("NAME");
            long expFromVerifiedIssuers = verified_issuers.getJSONObject( tdcaddr ).getLong("EXP_DATE");
            if(nameFromVerifiedIssuers.equals( nameFromQrImage ))
                if((System.currentTimeMillis() / 1000L) > expFromVerifiedIssuers)
                    html = html + "<div><img alt=\"can't load image\" src=\"images/goldseal.png\"><p style=\"color:pink\">Issuer verified but expired "+tdcaddr+"</p></div>";
                else
                    html = html + "<div><img alt=\"can't load image\" src=\"images/goldseal.png\"><p style=\"color:green\">Issuer verified "+tdcaddr+"</p></div>";
            else html = html + "<p style=\"color:red\">Issuer unverified "+tdcaddr+"</p>";
        } catch (JSONException e) {
            e.printStackTrace();
            html = html + "<p style=\"color:red\">Issuer unverified "+tdcaddr+"</p>";
        }

        try {

            html = html + "<p>\""+doctype(json.getInt("DOC_TYPE"))+"\"</p>";
            //ISSUER
            html = html + "<p><strong>Issuer: "+json.getJSONObject("ORG_ISSUER").getString("NAME")+"</strong></p>";
            html = html + "<p>Issuers Website: "+json.getJSONObject("ORG_ISSUER").getString("WWW")+"</p>";
            html = html + "<p>Issuers Email: "+json.getJSONObject("ORG_ISSUER").getString("EMAIL")+"</p>";
            html = html + "<p>Issuers Phone: "+json.getJSONObject("ORG_ISSUER").getString("PHONE")+"</p>";
            html = html + "<p>Issuers Fax: "+json.getJSONObject("ORG_ISSUER").getString("FAX")+"</p>";
            html = html + "<p>Issuers Country: "+json.getJSONObject("ORG_ISSUER").getString("COUNTRY")+"</p>";
            if(json.getJSONObject("ORG_ISSUER").getString("SUB_COUNTRY")!="")
                html = html + "<p>Issuers Sub Country: "+json.getJSONObject("ORG_ISSUER").getString("SUB_COUNTRY")+"</p>";
            html = html + "<p>Issuers Address: "+json.getJSONObject("ORG_ISSUER").getJSONArray("ADDRESS").getString(0)+"</p>";
            if(json.getJSONObject("ORG_ISSUER").getJSONArray("ADDRESS").optString(1,"")!="")
                html = html + "<p>Issuers Address: "+json.getJSONObject("ORG_ISSUER").getJSONArray("ADDRESS").getString(1)+"</p>";
            html = html + "<p></p>";
            //RECIPIENT
            if(json.getJSONObject("ORG_RECIPIENT").getString("NAME") != "") {
                html = html + "<p><strong>Recipient: " + json.getJSONObject("ORG_RECIPIENT").getString("NAME") + "</strong></p>";
                html = html + "<p>Recipients Website: " + json.getJSONObject("ORG_RECIPIENT").getString("WWW") + "</p>";
                html = html + "<p>Recipients Email: " + json.getJSONObject("ORG_RECIPIENT").getString("EMAIL") + "</p>";
                html = html + "<p>Recipients Phone: " + json.getJSONObject("ORG_RECIPIENT").getString("PHONE") + "</p>";
                html = html + "<p>Recipients Fax: " + json.getJSONObject("ORG_RECIPIENT").getString("FAX") + "</p>";
                html = html + "<p>Recipients Country: " + json.getJSONObject("ORG_RECIPIENT").getString("COUNTRY") + "</p>";

                if (json.getJSONObject("ORG_RECIPIENT").optString("SUB_COUNTRY","") != "")
                    html = html + "<p>Recipients Sub Country: " + json.getJSONObject("ORG_RECIPIENT").getString("SUB_COUNTRY") + "</p>";

                if(json.getJSONObject("ORG_RECIPIENT").optJSONArray("ADDRESS")!=null) {
                    html = html + "<p>Recipients Address: " + json.getJSONObject("ORG_RECIPIENT").getJSONArray("ADDRESS").getString(0)
                            + "</p>";
                    if (json.getJSONObject("ORG_RECIPIENT").getJSONArray("ADDRESS").getString(1) != "")
                        html = html + "<p>Recipients Address: " + json.getJSONObject("ORG_RECIPIENT").getJSONArray("ADDRESS").getString(1)
                                + "</p>";
                }
            }
            //DOCUMENT
            if(json.getJSONObject("DOCUMENT").optString("DOC_ID","") != "")
                html = html + "<p>Document ID: " + json.getJSONObject("DOCUMENT").getString("DOC_ID") + "</p>";
            if(json.getJSONObject("DOCUMENT").optString("DOC_LEVEL","") != "")
                html = html + "<p>Document level: " + json.getJSONObject("DOCUMENT").getString("DOC_LEVEL") + "</p>";
            if(json.getJSONObject("DOCUMENT").optJSONArray("ADDL_DOC")!=null)
                html = html + "<p>Additional docs: " + json.getJSONObject("DOCUMENT").getJSONArray("ADDL_DOC").toString() + "</p>";

            java.util.Date date_time = new java.util.Date(json.getJSONObject("DOCUMENT").getLong("DOC_ISSUE_DATE")*1000);
            html = html + "<p>Document issued on: " + date_time.toString() + "</p>";

            java.util.Date date_time2 = new java.util.Date(json.getJSONObject("DOCUMENT").getLong("DOC_EXP_DATE")*1000);
            html = html + "<p>Document expire on: " + date_time2.toString() + "</p>";

            //PRODUCT
            if(json.optJSONObject("PRODUCT")!=null & json.optJSONObject("PRODUCT").optString("NAME","")!="") {
                html = html + "<p><strong>Product name: " + json.getJSONObject("PRODUCT").getString("NAME") + "</strong></p>";
                html = html + "<p>Product description: " + json.getJSONObject("PRODUCT").getString("DESCRIPTION") + "</p>";
                html = html + "<p>Product categories: " + json.getJSONObject("PRODUCT").getJSONArray("CATEGORY").toString() + "</p>";
                if (json.getJSONObject("PRODUCT").optJSONArray("INGREDIENTS")!=null)
                    html = html + "<p>Product ingredients: " + json.getJSONObject("PRODUCT").getJSONArray("INGREDIENTS").toString()
                            + "</p>";
                if (json.getJSONObject("PRODUCT").optJSONObject("ENERGY")!=null)
                    html = html + "<p>Product energy: " + json.getJSONObject("PRODUCT").getJSONObject("ENERGY").toString()
                            + "</p>";
                if (json.getJSONObject("PRODUCT").optJSONArray("FAITH")!=null)
                    html = html + "<p>Product faith: " + json.getJSONObject("PRODUCT").getJSONArray("FAITH").toString()
                            + "</p>";
                if (json.getJSONObject("PRODUCT").optJSONArray("DIET")!=null)
                    html = html + "<p>Product diet: " + json.getJSONObject("PRODUCT").getJSONArray("DIET").toString() + "</p>";

                html = html + "<p>Product country of origin: " + json.getJSONObject("PRODUCT").optString("COUNTRY_OF_ORIGIN","NA") + "</p>";
                html = html + "<p>Product URL: " + json.getJSONObject("PRODUCT").optString("WWW","NO") + "</p>";
                //html = html + "<p><strong>Product qr image URL: " + json.getJSONObject("PRODUCT").getString("WWW") + "</strong></p>";
                html = html + "<p>Product qr image URL: " + json.getJSONObject("PRODUCT").getString("QR_PRODUCT_IMAGE_URL") + "</p>";
                html = html + "<p>Product images: " +
                        images2Html(json.getJSONObject("PRODUCT").getJSONArray("PRODUCT_IMAGES")) + "</p>";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return html;
    }
    public String loadStringFromAsset(String asset) {
        String json = null;
        try {
            InputStream is = getAssets().open(asset);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private String getHtml(String inject){
        return loadStringFromAsset("html/verify_head.html")+inject+loadStringFromAsset("html/verify_footer.html");
    }

    private void requestTxInfo() {
        this.activity = this;
        this.application = activity.getWalletApplication();
        this.config = application.getConfiguration();
        //this.fragmentManager = getFragmentManager();
        final RequestTxInfoTask.ResultCallback callback = new RequestTxInfoTask.ResultCallback() {

            @Override
            public void onResult(String rawTx) {
                //tv.setText(tv.getText()+"\n\nRaw TX:\n" + rawTx);
                try {
                    trans = new Transaction(Constants.NETWORK_PARAMETERS, hexStringToByteArray(rawTx));
                    //String completed = trans.isMature()? "Mature\n" : "Too fresh\n";
                    String fromAddress = trans.toString();
                    fromAddress = fromAddress.substring(fromAddress.indexOf("addr:")+5,fromAddress.indexOf("}"));
                    fromAddress.trim();
                    //tv.setText("Is it mature?:\n"+completed);
                    String pubk = trans.getOutput(0).getScriptPubKey().toString();
                    if(pubk.contains("[")&pubk.contains("]")){
                        pubk = pubk.substring(pubk.indexOf("[")+1,pubk.indexOf("]"));
                        byte[] pubk_b = Hex.decode(pubk);
                        pubk = new String(pubk_b);
                        try {
                            JSONObject jsonObject = new JSONObject(pubk);
                            fromAddress = fromAddress.replace("\n", "").replace("\r", "");
                            tv.loadDataWithBaseURL("file:///android_asset/html/",getHtml(json2html(jsonObject,fromAddress))
                                    ,"text/html","utf8","tdcoin-verify");
                            //tv.loadUrl("file:///android_asset/html/images/goldseal.png");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            tv.loadDataWithBaseURL("file:///android_asset/html/",
                                    getHtml("<p>This transaction is not eligible for verification.</p>"),
                                    "text/html","utf8","tdcoin-verify-error");
                        }
                    }
                    //tv.loadData(trans.toString(),"text/html","utf8");
                    //tv.loadData(pubk,"text/html","utf8");
                    //tv.setMovementMethod(new ScrollingMovementMethod());
                } catch (Exception e) {
                    e.printStackTrace();
                    tv.loadDataWithBaseURL("file:///android_asset/html", getHtml("Error occurred during verification."),"text/html","utf8","");
                }
            }

            @Override
            public void onFail(int messageResId, Object... messageArgs) {
                final DialogBuilder dialog = DialogBuilder.warn(activity,
                        R.string.verify_document_failed_title);
                dialog.setMessage(getString(messageResId, messageArgs));
                dialog.setPositiveButton(R.string.button_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        requestTxInfo();
                    }
                });
                dialog.setNegativeButton(R.string.button_dismiss, null);
                dialog.show();
            }
        };
        new RequestTxInfoTask(backgroundHandler, callback).RequestTxInfo(activity.getAssets(), trHex);
        //handler.post(requestTxInfoRunnable);
    }
    private final Runnable requestTxInfoRunnable = new Runnable() {
        @Override
        public void run() {
            requestTxInfo();
        }
    };
}
