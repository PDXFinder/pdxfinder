package org.pdxfinder.services.ds;

/*
 * Created by csaba on 19/01/2018.
 */

import org.apache.commons.lang3.StringUtils;
import org.pdxfinder.services.dto.DrugSummaryDTO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelForQuery {

    private Long modelId;
    private String datasource;
    private String datasourceName;
    private String externalId;
    private String patientAge;
    private String patientTreatmentStatus;
    private String patientGender;
    private String patientEthnicity;
    private String sampleOriginTissue;
    private String sampleSampleSite;
    private String sampleExtractionMethod;
    private String sampleClassification;
    private String sampleTumorType;
    private String modelImplantationSite;
    private String modelImplantationType;
    private Set<String> modelHostStrain;
    private List<String> cancerSystem;
    private String cancerOrgan;
    private String cancerCellType;
    private String diagnosis;
    private String mappedOntologyTerm;
    private String treatmentHistory;
    private List<String> mutatedVariants;
    private List<String> dataAvailable;
    private Set<String> allOntologyTermAncestors;

    private List<String> breastCancerMarkers;
    private List<String> geneExpression;
    private List<String> cytogenetics;

    private Set<String> queryMatch;



    private List<String> drugWithResponse;
    private List<String> patientTreatments;
    private List<String> cnaMarkers;

    private List<String> projects;
    private List<String> publications;
    private String modelAccessibility;
    private String accessModalities;


    public ModelForQuery() {
    }


    public String getBy(SearchFacetName facet) {
        String s;
        switch (facet) {
            case diagnosis:
                s = mappedOntologyTerm;
                break;
            case datasource:
                s = datasource;
                break;
            case patient_age:
                s = patientAge;
                break;
            case patient_treatment_status:
                s = patientTreatmentStatus;
                break;
            case patient_gender:
                s = patientGender;
                break;
            case sample_origin_tissue:
                s = sampleOriginTissue;
                break;
            case sample_classification:
                s = sampleClassification;
                break;
            case sample_tumor_type:
                s = sampleTumorType;
                break;
            case model_implantation_site:
                s = modelImplantationSite;
                break;
            case model_implantation_type:
                s = modelImplantationType;
                break;
            case model_host_strain:
                s = modelHostStrain.stream().collect(Collectors.joining("::"));;
                break;
            case organ:
                s = cancerOrgan;
                break;
            case cancer_system:
                // Pass back the list of top level ontology systems delimited by "::"
                s = cancerSystem.stream().collect(Collectors.joining("::"));
                break;
            case cell_type:
                s = cancerCellType;
                break;
            case project:
                if(projects != null){
                    s = projects.stream().collect(Collectors.joining("::"));
                }
                else{
                    s = null;
                }
                break;
            default:
                s = null;
                break;
        }
        return s;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientTreatmentStatus() {
        return patientTreatmentStatus;
    }

    public void setPatientTreatmentStatus(String patientTreatmentStatus) {
        this.patientTreatmentStatus = patientTreatmentStatus;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getSampleOriginTissue() {
        return sampleOriginTissue;
    }

    public void setSampleOriginTissue(String sampleOriginTissue) {
        this.sampleOriginTissue = sampleOriginTissue;
    }

    public String getSampleSampleSite() {
        return sampleSampleSite;
    }

    public void setSampleSampleSite(String sampleSampleSite) {
        this.sampleSampleSite = sampleSampleSite;
    }

    public String getSampleExtractionMethod() {
        return sampleExtractionMethod;
    }

    public void setSampleExtractionMethod(String sampleExtractionMethod) {
        this.sampleExtractionMethod = sampleExtractionMethod;
    }

    public String getSampleClassification() {
        return sampleClassification;
    }

    public void setSampleClassification(String sampleClassification) {
        this.sampleClassification = sampleClassification;
    }

    public String getSampleTumorType() {
        return sampleTumorType;
    }

    public void setSampleTumorType(String sampleTumorType) {
        this.sampleTumorType = sampleTumorType;
    }

    public String getModelImplantationSite() {
        return modelImplantationSite;
    }

    public void setModelImplantationSite(String modelImplantationSite) {
        this.modelImplantationSite = modelImplantationSite;
    }

    public String getModelImplantationType() {
        return modelImplantationType;
    }

    public void setModelImplantationType(String modelImplantationType) {
        this.modelImplantationType = modelImplantationType;
    }

    public Set<String> getModelHostStrain() {
        return modelHostStrain;
    }

    public void setModelHostStrain(Set<String> modelHostStrain) {
        this.modelHostStrain = modelHostStrain;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public List<String> getCancerSystem() {
        return cancerSystem;
    }

    public void setCancerSystem(List<String> cancerSystem) {
        this.cancerSystem = cancerSystem;
    }

    public String getCancerOrgan() {
        return cancerOrgan;
    }

    public void setCancerOrgan(String cancerOrgan) {
        this.cancerOrgan = cancerOrgan;
    }

    public String getCancerCellType() {
        return cancerCellType;
    }

    public void setCancerCellType(String cancerCellType) {
        this.cancerCellType = cancerCellType;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getMappedOntologyTerm() {
        return mappedOntologyTerm;
    }

    public void setMappedOntologyTerm(String mappedOntologyTerm) {
        this.mappedOntologyTerm = mappedOntologyTerm;
    }

    public String getTreatmentHistory() {
        return treatmentHistory;
    }

    public void setTreatmentHistory(String treatmentHistory) {
        this.treatmentHistory = treatmentHistory;
    }

    public List<String> getDataAvailable() {
        return dataAvailable;
    }

    public void setDataAvailable(List<String> dataAvailable) {
        this.dataAvailable = dataAvailable;
    }

    public Set<String> getAllOntologyTermAncestors() {
        return allOntologyTermAncestors;
    }

    public void setAllOntologyTermAncestors(Set<String> allOntologyTermAncestors) {
        this.allOntologyTermAncestors = allOntologyTermAncestors;
    }

    public Set<String> getQueryMatch() {
        return queryMatch;
    }

    public void setQueryMatch(Set<String> queryMatch) {
        this.queryMatch = queryMatch;
    }


    public List<String> getMutatedVariants() {
        return mutatedVariants;
    }

    public void setMutatedVariants(List<String> mutatedVariants) {
        this.mutatedVariants = mutatedVariants;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public List<String> getPublications() {
        return publications;
    }

    public void setPublications(List<String> publications) {
        this.publications = publications;
    }

    public String getFormattedQueryMatch(String query) {

        // Return nothing if there is no query
        if (query == null || query.length() < 1) {
            return null;
        }

        // Replace URL encoded spaces in the query string
        String normQuery = query.replaceAll("%20", " ").replaceAll("\\+", " ");

        // Return nothing if the query (case insensitively) matches the ontology term directly
        if (this.mappedOntologyTerm.toLowerCase().contains(normQuery.toLowerCase())) {
            return null;
        }

        // Return a string indicating which ontology term ancestors matches and highlight the match for the tooltip
        if (this.queryMatch != null && this.queryMatch.size() > 0) {
            List<String> s = new ArrayList<>(this.queryMatch);
            List<String> replaced = new ArrayList<>();
            for (String r : s) {
                // Find the case insensitive matched part of the term searched,
                // replace with original string surrounded with bold tags,
                // and do not allow wrapping at spaces
                replaced.add(r.replaceAll("(?i)(" + normQuery + ")", "<b>$1</b>").replaceAll(" ", "&nbsp;"));
            }
            return "Matches:<hr />" + StringUtils.join(replaced, "<br />");
        }
        return null;
    }



    public void addProject(String project){

        if(projects == null){
            projects = new ArrayList<>();
        }

        projects.add(project);

    }

    public String getPatientEthnicity() {
        return patientEthnicity;
    }

    public void setPatientEthnicity(String patientEthnicity) {
        this.patientEthnicity = patientEthnicity;
    }

    public String toString(){

        return "{model:"+modelId+"DS:"+datasource+ "}";
    }

    public List<String> getDrugWithResponse() {
        return drugWithResponse;
    }

    public void setDrugWithResponse(List<String> drugWithResponse) {
        this.drugWithResponse = drugWithResponse;
    }

    public void addDrugWithResponse(String drugWithResponse){

        if(this.drugWithResponse == null){
            this.drugWithResponse = new ArrayList<>();
        }

        this.drugWithResponse.add(drugWithResponse);
    }

    public void addMutatedVariant(String s){

        if(mutatedVariants == null){
            mutatedVariants = new ArrayList<>();
        }
        mutatedVariants.add(s);
    }

    public List<String> getBreastCancerMarkers() {
        return breastCancerMarkers;
    }

    public void setBreastCancerMarkers(List<String> breastCancerMarkers) {
        this.breastCancerMarkers = breastCancerMarkers;
    }


    public void addBreastCancerMarkers(String m){

        if(breastCancerMarkers == null){
            breastCancerMarkers = new ArrayList<>();
        }
        breastCancerMarkers.add(m);
    }

    public void addPatientTreatment(String t){

        if(patientTreatments == null) patientTreatments = new ArrayList<>();
        patientTreatments.add(t);
    }

    public List<String> getPatientTreatments() {
        return patientTreatments;
    }

    public void setPatientTreatments(List<String> patientTreatments) {
        this.patientTreatments = patientTreatments;
    }

    public String getModelAccessibility() {
        return modelAccessibility;
    }

    public void setModelAccessibility(String modelAccessibility) {
        this.modelAccessibility = modelAccessibility;
    }

    public String getAccessModalities() {
        return accessModalities;
    }

    public void setAccessModalities(String accessModalities) {
        this.accessModalities = accessModalities;
    }

    public List<String> getCnaMarkers() {
        return cnaMarkers;
    }

    public void setCnaMarkers(List<String> cnaMarkers) {
        this.cnaMarkers = cnaMarkers;
    }

    public void addCnaMarker(String s){

        if(this.cnaMarkers == null) this.cnaMarkers = new ArrayList<>();

        if(!this.cnaMarkers.contains(s)){
            this.cnaMarkers.add(s);
        }
    }

    public List<String> getGeneExpression() {
        return geneExpression;
    }

    public void setGeneExpression(List<String> geneExpression) {
        this.geneExpression = geneExpression;
    }

    public List<String> getCytogenetics() {
        return cytogenetics;
    }

    public void setCytogenetics(List<String> cytogenetics) {
        this.cytogenetics = cytogenetics;
    }

    public void addGeneExpression(String s){
        if(this.geneExpression == null) this.geneExpression = new ArrayList<>();
        if(!this.geneExpression.contains(s)){
            this.geneExpression.add(s);
        }
    }

    public void addCytogenetics(String s){
        if(this.cytogenetics == null) this.cytogenetics = new ArrayList<>();
        if(!this.cytogenetics.contains(s)){
            this.cytogenetics.add(s);
        }
    }
}
