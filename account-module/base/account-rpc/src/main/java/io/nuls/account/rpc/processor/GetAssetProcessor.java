package io.nuls.account.rpc.processor;

import io.nuls.account.model.Address;
import io.nuls.core.tools.cmd.CommandBuilder;
import io.nuls.core.tools.cmd.CommandHelper;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.processor.CommandProcessor;

/**
 * @author: Charlie
 * @date: 2018/5/25
 */
public class GetAssetProcessor implements CommandProcessor {


    @Override
    public String getCommand() {
        return "getasset";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address> address - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return  "getasset <address> --get your assets";
    }

    @Override
    public boolean argsValidate(String[] args) {
        int length = args.length;
        if(length != 2) {
            return false;
        }
        if (!CommandHelper.checkArgsIsNull(args)) {
            return false;
        }
        if (!Address.validAddress(args[1])) {
            return false;
        }
        return false;
    }

    @Override
    public Result execute(String[] args) {
        return null;
    }
}
