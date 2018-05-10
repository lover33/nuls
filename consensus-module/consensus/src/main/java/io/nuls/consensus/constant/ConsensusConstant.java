/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package io.nuls.consensus.constant;

import io.nuls.kernel.model.Na;

/**
 * Created by ln on 2018/5/7.
 */
public interface ConsensusConstant {

    /**
     * consensus module id
     */
    short MODULE_ID_CONSENSUS = 7;

    /**
     * unit:second
     */
    long BLOCK_TIME_INTERVAL_SECOND = 10;

    /**
     * unit:millis
     */
    long BLOCK_TIME_INTERVAL_MILLIS = BLOCK_TIME_INTERVAL_SECOND * 1000L;

    /**
     * default:2M
     */
    long MAX_BLOCK_SIZE = 2 << 21;

    /**
     * consensus transaction types
     */
    int TX_TYPE_REGISTER_AGENT = 90;
    int TX_TYPE_JOIN_CONSENSUS = 91;
    int TX_TYPE_CANCEL_DEPOSIT = 92;
    int TX_TYPE_STOP_AGENT = 95;
    int TX_TYPE_YELLOW_PUNISH = 93;
    int TX_TYPE_RED_PUNISH = 94;


}
