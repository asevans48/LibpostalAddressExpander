package com.si;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.libpostal.libpostal_normalize_options_t;
import org.junit.Test;

import static org.bytedeco.libpostal.global.postal.*;
import static org.bytedeco.libpostal.global.postal.libpostal_setup_language_classifier_datadir;

public class TestExpander {

    @Test
    public void shouldExpandAddress() throws Exception{
        String lpPath = "E:\\libpostal\\libpostal";
        libpostal_setup_datadir(lpPath);
        libpostal_setup_language_classifier_datadir(lpPath);
        libpostal_normalize_options_t options =  libpostal_get_default_options();
        String text = "Aurora IL";
        BytePointer address = new BytePointer(text, "UTF-8");
        SizeTPointer szptr = new SizeTPointer(0);
        PointerPointer result = libpostal_expand_address(address, options, szptr);
        long t_size = szptr.get();
        String[] results = new String[(int)t_size];
        for(long i = 0; i < t_size; i ++){
            results[(int) i] = result.getString(i);
        }
        address.deallocate();
        szptr.deallocate();
        result.deallocate();
        address = null;
        szptr = null;
        result = null;
        assert(results.length > 0);
    }

}
