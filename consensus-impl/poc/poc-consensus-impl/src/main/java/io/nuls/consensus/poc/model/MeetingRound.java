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
package io.nuls.consensus.poc.model;

import io.nuls.account.entity.Account;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.date.TimeService;
import io.nuls.protocol.constant.ProtocolConstant;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 * @date 2017/12/25
 */
public class MeetingRound {
    private Account localPacker;
    private double totalWeight;
    private long index;
    private long startTime;
    private int memberCount;
    private List<MeetingMember> memberList;
    private Map<String, Integer> addressOrderMap = new HashMap<>();
    private MeetingRound preRound;

    public MeetingRound getPreRound() {
        return preRound;
    }

    public void setPreRound(MeetingRound preRound) {
        this.preRound = preRound;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return startTime + memberCount * ProtocolConstant.BLOCK_TIME_INTERVAL_SECOND * 1000L;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public MeetingMember getMember(int order) {
        if (order == 0) {
            throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "the parameter is wrong:memberOrder");
        }
        if (null == memberList || memberList.isEmpty()) {
            throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "consensus member list is empty");
        }
        return this.memberList.get(order - 1);
    }

    public void setMemberList(List<MeetingMember> memberList) {
        this.memberList = memberList;
        if (null == memberList || memberList.isEmpty()) {
            throw new NulsRuntimeException(ErrorCode.DATA_ERROR, "consensus member list is empty");
        }
        this.memberCount = memberList.size();
        addressOrderMap.clear();
        for (int i = 0; i < memberList.size(); i++) {
            MeetingMember pmm = memberList.get(i);
            pmm.setRoundIndex(this.getIndex());
            pmm.setRoundStartTime(this.getStartTime());
            pmm.setPackingIndexOfRound(i + 1);
            addressOrderMap.put(pmm.getPackingAddress(), i + 1);
        }
    }

    public Integer getOrder(String address) {
        Integer val = addressOrderMap.get(address);
        if (null == val) {
            return null;
        }
        return val;
    }

    public MeetingMember getMember(String address) {
        Integer order = getOrder(address);
        if (null == order) {
            return null;
        }
        return getMember(order);
    }

    public Account getLocalPacker() {
        return localPacker;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }


    public double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public List<MeetingMember> getMemberList() {
        return memberList;
    }

    public void calcLocalPacker(List<Account> accountList) {
        for (Account account : accountList) {
            if (null != this.getOrder(account.getAddress().getBase58())) {
                this.localPacker = account;
                return;
            }
        }
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        for (MeetingMember member : this.getMemberList()) {
            str.append(member.getPackingAddress());
            str.append(" ,order:" + member.getPackingIndexOfRound());
            str.append(",packTime:" + new Date(member.getPackEndTime()));
            str.append(",creditVal:" + member.getRealCreditVal());
            str.append("\n");
        }
        if (null == this.getPreRound()) {
            return ("round:index:" + this.getIndex() + " , start:" + new Date(this.getStartTime())
                    + ", netTime:(" + new Date(TimeService.currentTimeMillis()).toString() + ") , members:\n :" + str);
        } else {
            return ("round:index:" + this.getIndex() + " ,preIndex:" + this.getPreRound().getIndex() + " , start:" + new Date(this.getStartTime())
                    + ", netTime:(" + new Date(TimeService.currentTimeMillis()).toString() + ") , members:\n :" + str);
        }
    }
}
