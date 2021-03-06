package org.pdxfinder.services.dto;

/*
 * Created by abayomi on 21/09/2018.
 */
public class CollectionEventsDTO {

    private String age;
    private String diagnosis;
    private String type;
    private String pdxMouse;
    private String data;
    private String collectionSite;

    public CollectionEventsDTO() {
    }

    public CollectionEventsDTO(String age,
                               String diagnosis,
                               String type,
                               String pdxMouse,
                               String data,
                               String collectionSite) {
        this.age = age;
        this.diagnosis = diagnosis;
        this.type = type;
        this.pdxMouse = pdxMouse;
        this.data = data;
        this.collectionSite = collectionSite;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPdxMouse() {
        return pdxMouse;
    }

    public void setPdxMouse(String pdxMouse) {
        this.pdxMouse = pdxMouse;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getCollectionSite() {
        return collectionSite;
    }

    public void setCollectionSite(String collectionSite) {
        this.collectionSite = collectionSite;
    }
}
