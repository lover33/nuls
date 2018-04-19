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
 */
package io.nuls.protocol.base.service.impl;

import io.nuls.account.entity.Address;
import io.nuls.consensus.poc.protocol.model.block.BlockRoundData;
import io.nuls.consensus.poc.protocol.service.BlockService;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.dto.Page;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.log.BlockLog;
import io.nuls.core.utils.log.Log;
import io.nuls.core.utils.spring.lite.annotation.Autowired;
import io.nuls.core.validate.ValidateResult;
import io.nuls.db.entity.BlockHeaderPo;
import io.nuls.db.transactional.annotation.DbSession;
import io.nuls.ledger.service.intf.LedgerService;
import io.nuls.protocol.constant.TxStatusEnum;
import io.nuls.protocol.context.NulsContext;
import io.nuls.protocol.model.Block;
import io.nuls.protocol.model.BlockHeader;
import io.nuls.protocol.model.NulsDigestData;
import io.nuls.protocol.model.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 * @date 2017/12/11
 */
public class BlockServiceImpl implements BlockService {
    private BlockStorageService blockStorageService = BlockStorageService.getInstance();
    @Autowired
    private LedgerService ledgerService;

    @Override
    public Block getGengsisBlock() {
        try {
            return blockStorageService.getBlock(0);
        } catch (Exception e) {
            Log.error(e);
            Log.error(e);
        }
        return null;
    }

    @Override
    public Block getLocalBestBlock() {
        long height = this.blockStorageService.getBestHeight();
        try {
            return blockStorageService.getBlock(height);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public BlockHeader getBlockHeader(long height) throws NulsException {
        return blockStorageService.getBlockHeader(height);
    }

    @Override
    public BlockHeader getBlockHeader(String hash) throws NulsException {
        return blockStorageService.getBlockHeader(hash);
    }

    @Override
    public Block getBlock(String hash) {
        Block block = null;
        try {
            block = blockStorageService.getBlock(hash);
        } catch (Exception e) {
            Log.error(e);
        }
        return block;
    }

    @Override
    public Block getBlock(long height) {
        try {
            return blockStorageService.getBlock(height);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public List<Block> getBlockList(long startHeight, long endHeight) throws NulsException {
        List<Block> blockList = blockStorageService.getBlockList(startHeight, endHeight);
        return blockList;
    }


    @Override
    @DbSession
    public boolean saveBlock(Block block) throws IOException {
        BlockLog.debug("save block height:" + block.getHeader().getHeight() + ", preHash:" + block.getHeader().getPreHash() + " , hash:" + block.getHeader().getHash() + ", address:" + Address.fromHashs(block.getHeader().getPackingAddress()));
        for (int x = 0; x < block.getHeader().getTxCount(); x++) {
            Transaction tx = block.getTxs().get(x);
            tx.setIndex(x);
            tx.setBlockHeight(block.getHeader().getHeight());
            try {
                tx.verifyWithException();
                ledgerService.commitTx(tx, block);
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(e);
            }
        }
        ledgerService.saveTxList(block.getTxs());
        blockStorageService.save(block);
        return true;
    }


    @Override
    @DbSession
    public boolean rollbackBlock(String hash) {
        Block block = this.getBlock(hash);
        if (null == block) {
            return false;
        }
        this.rollback(block.getTxs(), block.getTxs().size() - 1);
        this.ledgerService.deleteTx(block.getHeader().getHeight());
        blockStorageService.delete(block.getHeader().getHash().getDigestHex());
        NulsContext.getInstance().setBestBlock(this.getBestBlock());
        return true;
    }


    @Override
    public Page<BlockHeaderPo> getBlockHeaderList(String nodeAddress, int type, int pageNumber, int pageSize) {
        return blockStorageService.getBlocListByAddress(nodeAddress, type, pageNumber, pageSize);
    }

    @Override
    public Page<BlockHeaderPo> getBlockHeaderList(int pageNumber, int pageSize) {
        return blockStorageService.getBlockHeaderList(pageNumber, pageSize);
    }

    @Override
    public List<BlockHeaderPo> getBlockHeaderList(long startHeight, long endHeight) {
        return blockStorageService.getBlockHeaderList(startHeight, endHeight);
    }


    @Override
    public long getPackingCount(String address) {
        return blockStorageService.getBlockCount(address, -1L, -1L, 0L);
    }

    private void rollback(List<Transaction> txs, int max) {
        int i = max;
        if (max >= txs.size()) {
            i = txs.size() - 1;
        }
        for (; i >= 0; i--) {
            Transaction tx = txs.get(i);
//todo            if (tx.getType() != TransactionConstant.TX_TYPE_COIN_BASE && tx.getType() != TransactionConstant.TX_TYPE_YELLOW_PUNISH && tx.getType() != TransactionConstant.TX_TYPE_RED_PUNISH) {
//                txCacheManager.putTxToOrphanCache(tx);
//            }
            if (tx.getStatus() == TxStatusEnum.AGREED && !ledgerService.checkTxIsMySend(tx)) {
                continue;
            }
            try {
                ledgerService.rollbackTx(tx, null);
            } catch (NulsException e) {
                Log.error(e);
            }

        }

    }

    @Override
    public Block getBestBlock() {

        return this.getLocalBestBlock();
    }


    @Override
    public List<BlockHeaderPo> getBlockHashList(long start, long end) {
        return blockStorageService.getBlockHashList(start, end);
    }


    @Override
    public Block getBlockFromMyChain(String hash) {
        try {
            return blockStorageService.getBlock(hash);
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getSumTxCount(String packingAddress, long startRoundIndex, long endRoundIndex) {
        return blockStorageService.getSumTxCount(packingAddress, startRoundIndex, endRoundIndex);
    }

    @Override
    public List<BlockHeaderPo> getBlockHeaderListByRound(long startRoundIndex, long endRoundIndex) {
        return this.blockStorageService.getBlockHeaderListByRound(startRoundIndex, endRoundIndex);
    }
}
