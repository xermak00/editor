package cz.incad.kramerius.editor.share.rpc;


import net.customware.gwt.dispatch.shared.Result;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GetTabstitleResult implements Result, IsSerializable {

    private String title;

    /* gwt serialization purposes */
    private GetTabstitleResult() {}

    public GetTabstitleResult(String title) {
        super();
        this.title = title;
    }

    
    public String getTitle() {
        return title;
    }
}
