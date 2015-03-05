package cz.incad.kramerius.editor.share.rpc;

import net.customware.gwt.dispatch.shared.Action;

public class GetTabstitleQuery implements Action<GetTabstitleResult> {

    private String pid;

    /* gwt serialization purposes */
    private GetTabstitleQuery() {}

    public GetTabstitleQuery(String pid) {
        this.pid = pid;
    }

    public String getPID() {
        return pid;
    }
}
