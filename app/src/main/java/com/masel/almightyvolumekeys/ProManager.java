package com.masel.almightyvolumekeys;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.masel.rec_utils.Utils;

import java.util.ArrayList;
import java.util.List;

class ProManager implements PurchasesUpdatedListener {

    private interface RunnableWithProductDetails {
        void run(SkuDetails skuDetails);
    }

    private Activity activity;
    private BillingClient billingClient;

    /**
     * State-actions, executed on initialization, and after purchase state changed. */
    private Runnable onLocked = null;
    private Runnable onPending = null;
    private Runnable onUnlocked = null;

    ProManager(Activity activity) {
        this.activity = activity;
        this.billingClient = BillingClient.newBuilder(activity)
                .enablePendingPurchases()
                .setListener(this)
                .build();
    }



    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (purchases == null || purchases.size() == 0) return;

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            handlePurchase(purchases.get(0));
        }
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Utils.log("Billing ended by user");
        }
        else {
            // Handle any other error codes.
            Utils.log("Billing ended, unknown reason: " + billingResult);
        }
    }

    /**
     * Execute state-action. Also handle acknowledgment of purchase.
     * @param purchase
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            onUnlocked.run();
            acknowledgePurchase(purchase);
        }
        else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            onPending.run();
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        if (purchase.isAcknowledged()) return;

        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                // ...
            }
        });
    }

    void setStateActions(Runnable onLocked, Runnable onPending, Runnable onUnlocked) {
        this.onLocked = onLocked;
        this.onPending = onPending;
        this.onUnlocked = onUnlocked;
    }

    /**
     * Initializes the connection to google play and executes state-action depending on current pro-state.
     * Network not needed.
     */
    void init() {
        Runnable onConnected = () -> {
            List<Purchase> purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
            if (purchases.size() == 0) onLocked.run();
            else handlePurchase(purchases.get(0));
        };

        connectAndExecute(onConnected);
    }

    /**
     * Network needed, else ends with toast.
     */
    void coldStartPurchase() {

        RunnableWithProductDetails onProductDetailsFetched = skuDetails -> {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
            BillingResult res = billingClient.launchBillingFlow(activity, flowParams);
            if (res.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Utils.logAndToast(activity, "Are you connected to internet?");
            }
        };

        Runnable onConnected = () -> {
            List<String> skuList = new ArrayList<>();
            skuList.add("com.masel.almightyvolumekeys.product_id_unlock_pro");
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);

            billingClient.querySkuDetailsAsync(params.build(),
                    new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(BillingResult result, List<SkuDetails> skuDetailsList) {
                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                                onProductDetailsFetched.run(skuDetailsList.get(0));
                            }
                            else {
                                Utils.logAndToast(activity, "Are you connected to internet?");
                            }
                        }
                    });
        };

        connectAndExecute(onConnected);
    }

    /**
     * Connect to play-store service and execute action. If already connected, just execute action.
     * If error, end with toast.
     */
    private void connectAndExecute(Runnable onConnected) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    onConnected.run();
                }
                else {
                    Utils.logAndToast(activity, "Can't connect to google play.");
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Utils.log("Connection with google play lost");
            }
        });
    }

    void destroy() {
        billingClient.endConnection();
    }

    // region Save and load is-locked-flag.

    private static final String KEY_IS_LOCKED = "com.masel.almightyvolumekeys.KEY_IS_LOCKED";
    static void saveIsLocked(Context context, boolean isLocked) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_IS_LOCKED, isLocked)
                .apply();
    }
    static boolean loadIsLocked(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_IS_LOCKED, true);
    }

    // endregion
}
