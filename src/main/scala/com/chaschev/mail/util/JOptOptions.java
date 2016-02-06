package com.chaschev.mail.util;

import chaschev.lang.OpenBean;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.internal.AbbreviationMap;

import java.util.List;

/**
 * Created by andrey on 2/5/16.
 */
public class JOptOptions {
    protected final static OptionParser parser = new OptionParser();

    protected final OptionSet optionSet;

    public JOptOptions(String[] args) {
        optionSet = parser.parse(args);
    }

    public <T> T get(OptionSpec<T> optionSpec) {
        return optionSet.valueOf(optionSpec);
    }

    public <T> List<T> getList(OptionSpec<T> optionSpec) {
        return optionSet.valuesOf(optionSpec);
    }

    public String printHelpOn(){
        return printHelpOn(160, 2);
    }

    public String printHelpOn(int desiredOverallWidth, int desiredColumnSeparatorWidth)  {
//        try {
        return new BuiltinHelpFormatter(desiredOverallWidth, desiredColumnSeparatorWidth).format(((AbbreviationMap) OpenBean.getFieldValue(
                parser, "recognizedOptions")).toJavaUtilMap());
//            parser.printHelpOn(sink);
//        } catch (IOException e) {
//            throw Exceptions.runtime(e);
//        }
    }

    public boolean has(OptionSpec<?> optionSpec) {
        return optionSet.has(optionSpec);
    }

    public OptionSet getOptionSet() {
        return optionSet;
    }
}
