/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.accountLedger.storage.service.impl;

import com.sun.org.apache.regexp.internal.RE;
import io.nuls.account.model.Address;
import io.nuls.accountLedger.constant.AccountLedgerErrorCode;
import io.nuls.accountLedger.storage.constant.AccountLedgerStorageConstant;
import io.nuls.accountLedger.storage.po.TransactionInfoPo;
import io.nuls.accountLedger.storage.service.AccountLedgerStorageService;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.db.service.BatchOperation;
import io.nuls.db.service.DBService;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.exception.NulsRuntimeException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.*;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.VarInt;
import org.spongycastle.util.Arrays;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * author Facjas
 * date 2018/5/10.
 */
@Component
public class AccountLedgerStorageServiceImpl implements AccountLedgerStorageService {
    /**
     * 通用数据存储服务
     * Universal data storage services.
     */
    @Autowired
    private DBService dbService;

    public AccountLedgerStorageServiceImpl() {

        Result result = dbService.createArea(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TRANSACTION);
        if (result.isFailed()) {
            //TODO
        }

        result = dbService.createArea(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_COINDATA);
        if (result.isFailed()) {
            //TODO
        }

        result = dbService.createArea(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TXINFO);
        if (result.isFailed()) {
            //TODO
        }
    }

    @Override
    public Result saveLocalTx(Transaction tx) {
        if (tx == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }
        byte[] txHashBytes = new byte[0];
        try {
            txHashBytes = tx.getHash().serialize();
        } catch (IOException e) {
            throw new NulsRuntimeException(e);
        }
        CoinData coinData = tx.getCoinData();
        if (coinData != null) {
            // delete - from
            List<Coin> froms = coinData.getFrom();
            BatchOperation batch = dbService.createWriteBatch(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_COINDATA);
            for (Coin from : froms) {
                batch.delete(from.getOwner());
            }
            // save utxo - to
            List<Coin> tos = coinData.getTo();
            byte[] indexBytes;
            for (int i = 0, length = tos.size(); i < length; i++) {
                try {
                    batch.put(Arrays.concatenate(txHashBytes, new VarInt(i).encode()), tos.get(i).serialize());
                } catch (IOException e) {
                    throw new NulsRuntimeException(e);
                }
            }

            Result batchResult = batch.executeBatch();
            if (batchResult.isFailed()) {
                return batchResult;
            }
        }

        Result result = null;
        try {
            result = dbService.put(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TRANSACTION, tx.getHash().serialize(), tx.serialize());
        } catch (IOException e) {
            throw new NulsRuntimeException(e);
        }
        return result;
    }

    @Override
    public Result<Integer> saveLocalTxInfo(TransactionInfoPo infoPo,List<byte[]> addresses) {
        if (infoPo == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }

        if(addresses == null || addresses.size()==0){
            return Result.getSuccess().setData(new Integer(0));
        }

        List<byte[]> savedKeyList = new ArrayList<>();

        try {
            for (int i = 0; i < addresses.size(); i++) {
                byte[] infoKey = new byte[AddressTool.HASH_LENGTH + infoPo.getTxHash().size()];
                System.arraycopy(addresses.get(i), 0, infoKey, 0, AddressTool.HASH_LENGTH);
                System.arraycopy(infoPo.getTxHash().getWholeBytes(), 0, infoKey, AddressTool.HASH_LENGTH, infoPo.getTxHash().size());
                dbService.put(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TXINFO, infoKey, infoPo.serialize());
                savedKeyList.add(infoKey);
            }
        }catch (IOException e){
            for(int i = 0; i<savedKeyList.size(); i++){
                dbService.delete(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TXINFO, savedKeyList.get(i));
            }

            return Result.getFailed(AccountLedgerErrorCode.IO_ERROR);
        }
        return Result.getSuccess().setData(new Integer(addresses.size()));
    }

    @Override
    public Result deleteLocalTxInfo(TransactionInfoPo infoPo) {
        byte[] infoBytes = null;
        if (infoPo == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);

        }

        try{
            infoBytes = infoPo.serialize();
        }catch (IOException e){
            return Result.getFailed(AccountLedgerErrorCode.IO_ERROR);
        }

        if(ArraysTool.isEmptyOrNull(infoBytes)){
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }

        byte[] addresses = infoPo.getAddresses();
        if (addresses.length % AddressTool.HASH_LENGTH != 0) {
            return Result.getFailed(KernelErrorCode.PARAMETER_ERROR);
        }

        int addressCount = addresses.length / AddressTool.HASH_LENGTH;

        for (int i = 0; i < addressCount; i++) {

            byte[] infoKey = new byte[AddressTool.HASH_LENGTH + infoPo.getTxHash().size()];
            System.arraycopy(addresses, i * AddressTool.HASH_LENGTH, infoKey, 0, AddressTool.HASH_LENGTH);
            System.arraycopy(infoPo.getTxHash().getWholeBytes(), 0, infoKey, AddressTool.HASH_LENGTH, infoPo.getTxHash().size());
            dbService.delete(AccountLedgerStorageConstant.DB_AREA_ACCOUNTLEDGER_TXINFO, infoKey);

        }

        return Result.getSuccess().setData(new Integer(addressCount));
    }

    @Override
    public Transaction getLocalTx(NulsDigestData hash) {
        return null;
    }

    @Override
    public Result deleteLocalTx(Transaction tx) {
        return null;
    }

    @Override
    public byte[] getCoinBytes(byte[] owner) {
        return new byte[0];
    }

    @Override
    public byte[] getTxBytes(byte[] txBytes) {
        return new byte[0];
    }
}
