package org.wso2.carbon.la.commons.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "response")
public class ResponseBean {

    /**
     * draw.
     */
    @XmlElement(required = false, name = "draw")
    private int draw;

    /**
     * recordsTotal.
     */
    @XmlElement(required = false, name = "recordsTotal")
    private int recordsTotal;

    /**
     * recordsFiltered.
     */
    @XmlElement(required = false, name = "recordsFiltered")
    private int recordsFiltered;

    /**
     * data.
     */
    @XmlElement(required = false, name = "data")
    private List<RecordBean> data;

    public List<RecordBean> getData() {
        return data;
    }

    public void setData(List<RecordBean> data) {
        this.data = data;
    }

    public int getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(int recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public int getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(int recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public int getDraw() {
        return draw;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

}
