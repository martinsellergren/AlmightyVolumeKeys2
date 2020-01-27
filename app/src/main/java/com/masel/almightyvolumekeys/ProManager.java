package com.masel.almightyvolumekeys;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
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
     * State-actions, executed on initialization, and after purchase state changed.
     * Could be called multiple times consecutively (must be actions where this is not a problem). */
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
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchases == null || purchases.size() == 0) return;
            handlePurchase(purchases.get(0));
        }
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Utils.log("Billing ended by user");
            onLocked.run();
        }
        else {
            // Handle any other error codes.
            Utils.log("Billing ended, unknown reason: " + billingResult);
            onLocked.run();
        }
    }

    /**
     * Execute state-action. Also handle acknowledgment of purchase.
     * @param purchase
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Utils.log("Successful purchase");
            onUnlocked.run();
            acknowledgePurchase(purchase);
        }
        else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Utils.log("Pending purchase");
            onPending.run();
        }
        else {
            Utils.log("Canceled purchase?");
            onLocked.run();
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        if (purchase.isAcknowledged()) return;

        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            Utils.log("Purchase acknowledged");
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
    void startPurchase() {
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
            //skuList.add("android.test.purchased");
            //skuList.add("android.test.canceled");
            //skuList.add("android.test.item_unavailable");
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);

            billingClient.querySkuDetailsAsync(params.build(),
                    (result, skuDetailsList) -> {
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null && skuDetailsList.size() != 0) {
                            onProductDetailsFetched.run(skuDetailsList.get(0));
                        }
                        else {
                            Utils.logAndToast(activity, "Are you connected to internet?");
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
        if (billingClient.isReady()) {
            onConnected.run();
        }
        else {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        onConnected.run();
                    } else {
                        Utils.logAndToast(activity, "Can't connect to google play.");
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Utils.log("Connection with google play lost");
                }
            });
        }
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

    /**
     * For testing.
     */
    void revertPro() {
        Runnable onConnected = () -> {
            List<Purchase> purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
            if (purchases.size() == 0) return;

            Purchase purchase = purchases.get(0);
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

            billingClient.consumeAsync(consumeParams, (billingResult, s) -> {
                Utils.log("Pro reverted");
            });
        };

        connectAndExecute(onConnected);
    }
}