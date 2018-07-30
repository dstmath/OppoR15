package com.android.internal.telephony;

public class OppoTxRxInfo {
    private OppoRxChainInfo mRxChain0;
    private int mRxChain0Valid;
    private OppoRxChainInfo mRxChain1;
    private int mRxChain1Valid;
    private OppoTxInfo mTx;
    private int mTxValid;

    public OppoTxRxInfo(int mRxChain0Valid, OppoRxChainInfo mRxChain0, int mRxChain1Valid, OppoRxChainInfo mRxChain1, int mTxValid, OppoTxInfo mTx) {
        this.mRxChain0Valid = mRxChain0Valid;
        this.mRxChain0 = mRxChain0;
        this.mRxChain1Valid = mRxChain1Valid;
        this.mRxChain1 = mRxChain1;
        this.mTxValid = mTxValid;
        this.mTx = mTx;
    }

    public int getRxChain0Valid() {
        return this.mRxChain0Valid;
    }

    public int getRxChain1Valid() {
        return this.mRxChain1Valid;
    }

    public int getTxValid() {
        return this.mTxValid;
    }

    public OppoRxChainInfo getRxChain0() {
        return this.mRxChain0;
    }

    public OppoRxChainInfo getRxChain1() {
        return this.mRxChain1;
    }

    public OppoTxInfo getTx() {
        return this.mTx;
    }

    public String toString() {
        return "mRxChain0Valid=" + this.mRxChain0Valid + ", mRxChain0=(" + this.mRxChain0.toString() + "), mRxChain1Valid=" + this.mRxChain1Valid + ", mRxChain1=(" + this.mRxChain1.toString() + ")," + "mTxValid=" + this.mTxValid + ", mTx=(" + this.mTx.toString() + ")";
    }
}
