package com.webank.wecross.routine.htlc;

import com.webank.wecross.account.Account;
import com.webank.wecross.common.ResourceQueryStatus;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.peer.Peer;
import com.webank.wecross.resource.EventCallback;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Driver;
import com.webank.wecross.stub.Request;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.Response;
import com.webank.wecross.stub.TransactionContext;
import com.webank.wecross.stub.TransactionRequest;
import com.webank.wecross.stub.TransactionResponse;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTLCResource extends Resource {

    private Logger logger = LoggerFactory.getLogger(HTLCResource.class);

    private Resource originResource;
    private Account account;
    private String path;

    public HTLCResource(Resource originResource) {
        this.originResource = originResource;
    }

    @Override
    public TransactionResponse call(TransactionContext<TransactionRequest> request) {
        //        TransactionContext<TransactionRequest> newRequest;
        //        try {
        //            newRequest = handleCallRequest(request);
        //        } catch (WeCrossException e) {
        //            TransactionResponse transactionResponse = new TransactionResponse();
        //            transactionResponse.setErrorCode(e.getErrorCode());
        //            transactionResponse.setErrorMessage(e.getMessage());
        //            return transactionResponse;
        //        }
        return originResource.call(request);
    }

    @Override
    public TransactionResponse sendTransaction(TransactionContext<TransactionRequest> request) {
        TransactionContext<TransactionRequest> newRequest;
        try {
            newRequest = handleSendTransactionRequest(request);
        } catch (WeCrossException e) {
            TransactionResponse transactionResponse = new TransactionResponse();
            transactionResponse.setErrorCode(e.getErrorCode());
            transactionResponse.setErrorMessage(e.getMessage());
            return transactionResponse;
        }

        return originResource.sendTransaction(newRequest);
    }

    public TransactionContext<TransactionRequest> handleSendTransactionRequest(
            TransactionContext<TransactionRequest> transactionContext) throws WeCrossException {
        TransactionRequest request = transactionContext.getData();
        if (request.getMethod().equals("unlock")) {
            String[] args = request.getArgs();
            if (args == null || args.length < 2) {
                logger.error("format of request is error in sendTransaction for unlock");
                throw new WeCrossException(
                        ResourceQueryStatus.ASSET_HTLC_REQUEST_ERROR,
                        "hash of lock transaction not found");
            }
            String transactionHash = args[0];

            AssetHTLC assetHTLC = new AssetHTLC();
            // Verify that the asset is locked
            if (!assetHTLC
                    .verifyLock(originResource, transactionHash)
                    .trim()
                    .equalsIgnoreCase("true")) {
                throw new WeCrossException(
                        ResourceQueryStatus.ASSET_HTLC_VERIFY_LOCK_ERROR,
                        "verify transaction of lock failed");
            }
            request.setArgs(Arrays.copyOfRange(args, 1, args.length - 1));
        }

        transactionContext.setData(request);
        logger.info("HTLCRequest: {}", transactionContext.toString());
        return transactionContext;
    }

    //    public TransactionContext<TransactionRequest> handleCallRequest(
    //            TransactionContext<TransactionRequest> transactionContext) throws WeCrossException
    // {
    //        TransactionRequest request = transactionContext.getData();
    //        if (request.getMethod().equals("getSecret")) {
    //            if (request.isFromP2P()) {
    //                throw new WeCrossException(
    //                        ResourceQueryStatus.ASSET_HTLC_NO_PERMISSION,
    //                        "cannot call getSecret by rpc interface");
    //            }
    //        }
    //        transactionContext.setData(request);
    //        return transactionContext;
    //    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Resource getOriginResource() {
        return originResource;
    }

    public void setOriginResource(Resource originResource) {
        this.originResource = originResource;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public void addConnection(Peer peer, Connection connection) {
        originResource.addConnection(peer, connection);
    }

    @Override
    public void removeConnection(Peer peer) {
        originResource.removeConnection(peer);
    }

    @Override
    public boolean isConnectionEmpty() {
        return originResource.isConnectionEmpty();
    }

    @Override
    public Response onRemoteTransaction(Request request) {
        Driver driver = getDriver();
        if (driver.isTransaction(request)) {
            TransactionContext<TransactionRequest> context =
                    driver.decodeTransactionRequest(request.getData());
            TransactionRequest transactionRequest = context.getData();
            if (transactionRequest.getMethod().equals("getSecret")) {
                Response response = new Response();
                response.setErrorCode(ResourceQueryStatus.ASSET_HTLC_NO_PERMISSION);
                response.setErrorMessage("cannot call getSecret by rpc interface");
                return response;
            }
        }
        return originResource.onRemoteTransaction(request);
    }

    @Override
    public void registerEventHandler(EventCallback callback) {
        originResource.registerEventHandler(callback);
    }

    @Override
    public String getType() {
        return originResource.getType();
    }

    @Override
    public void setType(String type) {
        originResource.setType(type);
    }

    @Override
    public String getChecksum() {
        return originResource.getChecksum();
    }

    @Override
    public Driver getDriver() {
        return originResource.getDriver();
    }

    @Override
    public void setDriver(Driver driver) {
        originResource.setDriver(driver);
    }

    @Override
    public ResourceInfo getResourceInfo() {
        return originResource.getResourceInfo();
    }

    @Override
    public void setResourceInfo(ResourceInfo resourceInfo) {
        originResource.setResourceInfo(resourceInfo);
    }

    @Override
    public boolean isHasLocalConnection() {
        return originResource.isHasLocalConnection();
    }
}
