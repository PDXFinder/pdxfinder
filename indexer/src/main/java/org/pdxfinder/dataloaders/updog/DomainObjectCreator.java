package org.pdxfinder.dataloaders.updog;

import org.pdxfinder.graph.dao.*;
import org.pdxfinder.services.DataImportService;
import org.pdxfinder.services.dto.NodeSuggestionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.*;

public class DomainObjectCreator {

    private Map<String, Table> pdxDataTables;
    //nodeType=>ID=>NodeObject
    private Map<String, Map<String, Object>> domainObjects;

    private DataImportService dataImportService;
    private static final Logger log = LoggerFactory.getLogger(DomainObjectCreator.class);

    private static final String PATIENT_KEY = "patient";
    private static final String PROVIDER_KEY = "provider_group";
    private static final String MODEL_KEY = "model";
    private static final String TUMOR_TYPE_KEY = "tumor_type";
    private static final String TISSUE_KEY = "tissue";
    private static final String HOST_STRAIN_KEY = "hoststrain";
    private static final String ENGRAFTMENT_SITE_KEY = "engraftment_site";
    private static final String ENGRAFTMENT_TYPE_KEY = "engraftment_type";
    private static final String ENGRAFTMENT_MATERIAL_KEY = "engraftment_material";
    private static final String PLATFORM_KEY = "platform";

    private static final String NOT_SPECIFIED = "Not Specified";


    public DomainObjectCreator(
            DataImportService dataImportService,
            Map<String, Table> pdxDataTables
    ) {
        this.dataImportService = dataImportService;
        this.pdxDataTables = pdxDataTables;
        domainObjects = new HashMap<>();
    }

    public void loadDomainObjects() {
        //: Do not change the order of these unless you want to risk 1. the universe to collapse OR 2. missing nodes in the db

        createProvider();
        createPatientData();
        createModelData();
        createSampleData();
        createSharingData();

        createSamplePlatformData();

        createTreatmentData();

        createMolecularData();

        persistNodes();
    }

    void createProvider() {
        log.info("Creating provider");
        Table finderRelatedTable = pdxDataTables.get("metadata-loader.tsv");
        Row row = finderRelatedTable.row(0);

        String providerName = row.getString(TSV.Metadata.name.name());
        String abbrev = row.getString(TSV.Metadata.abbreviation.name());
        String internalUrl = row.getString(TSV.Metadata.internal_url.name());

        Group providerGroup = dataImportService.getProviderGroup(
                providerName, abbrev, "", "", "", internalUrl);

        addDomainObject(PROVIDER_KEY, null, providerGroup);
    }

    void createPatientData() {
        log.info("Creating patient data");
        Table patientTable = pdxDataTables.get("metadata-patient.tsv");
        for (Row row : patientTable) {
            try {
                Patient patient = dataImportService.createPatient(
                        row.getText(TSV.Metadata.patient_id.name()),
                        (Group) getDomainObject(TSV.Metadata.provider_group.name(), null),
                        row.getText(TSV.Metadata.sex.name()),
                        "",
                        row.getText(TSV.Metadata.ethnicity.name()));

                patient.setCancerRelevantHistory(row.getText(TSV.Metadata.history.name()));
                patient.setFirstDiagnosis(row.getText(TSV.Metadata.initial_diagnosis.name()));
                patient.setAgeAtFirstDiagnosis(row.getText(TSV.Metadata.age_at_initial_diagnosis.name()));

                addDomainObject(
                        PATIENT_KEY,
                        row.getText(TSV.Metadata.patient_id.name()),
                        dataImportService.savePatient(patient));
            } catch (Exception e) {
                log.error(
                        "Error loading patient {} at row {}",
                        row.getText(TSV.Metadata.patient_id.name()),
                        row.getRowNumber());
            }
        }
    }

    void createSampleData() {
        log.info("Creating sample data");
        Table sampleTable = pdxDataTables.get("metadata-sample.tsv");
        for (Row row : sampleTable) {
            String patientId = row.getString(TSV.Metadata.patient_id.name());
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String dateOfCollection = row.getString(TSV.Metadata.collection_date.name());
            String ageAtCollection = row.getString(TSV.Metadata.age_in_years_at_collection.name());
            String collectionEvent = row.getString(TSV.Metadata.collection_event.name());
            String elapsedTime = row.getString(TSV.Metadata.months_since_collection_1.name());
            String primarySiteName = row.getString(TSV.Metadata.primary_site.name());
            String virologyStatus = row.getString(TSV.Metadata.virology_status.name());
            String treatmentNaive = row.getString(TSV.Metadata.treatment_naive_at_collection.name());

            Patient patient = (Patient) getDomainObject(PATIENT_KEY, patientId);
            if (patient == null) {
                log.error("Patient not found {}", patientId);
                throw new NullPointerException();}

            PatientSnapshot patientSnapshot = patient.getSnapShotByCollection(
                    ageAtCollection,
                    dateOfCollection,
                    collectionEvent,
                    elapsedTime);

            if (patientSnapshot == null) {
                patientSnapshot = new PatientSnapshot(
                        patient,
                        ageAtCollection,
                        dateOfCollection,
                        collectionEvent,
                        elapsedTime);
                patientSnapshot.setVirologyStatus(virologyStatus);
                patientSnapshot.setTreatmentNaive(treatmentNaive);
                patient.addSnapshot(patientSnapshot);
            }

            Sample sample = createPatientSample(row);
            patientSnapshot.addSample(sample);

            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODEL_KEY, modelId);
            if (modelCreation == null) throw new NullPointerException();

            modelCreation.setSample(sample);
            modelCreation.addRelatedSample(sample);

        }
    }

    void createModelData() {
        log.info("Creating model data");
        Table modelTable = pdxDataTables.get("metadata-model.tsv");
        Group providerGroup = (Group) domainObjects.get(PROVIDER_KEY).get(null);
        for (Row row : modelTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String hostStrainNomenclature = row.getString(TSV.Metadata.host_strain_full.name());
            String passageNum = row.getString(TSV.Metadata.passage_number.name());

            ModelCreation modelCreation = new ModelCreation();
            modelCreation.setSourcePdxId(modelId);
            modelCreation.setDataSource(providerGroup.getAbbreviation());
            addDomainObject(MODEL_KEY, modelId, modelCreation);

            Specimen specimen = modelCreation.getSpecimenByPassageAndHostStrain(passageNum, hostStrainNomenclature);
            if (specimen == null) {
                specimen = createSpecimen(row, row.getRowNumber());
                modelCreation.addSpecimen(specimen);
                modelCreation.addRelatedSample(specimen.getSample());
            }
        }
        createModelValidationData();
    }

    void createModelValidationData() {

        Table modelValidationTable = pdxDataTables.get("metadata-model_validation.tsv");
        for (Row row : modelValidationTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String validationTechnique = row.getString(TSV.Metadata.validation_technique.name());
            String description = row.getString(TSV.Metadata.description.name());
            String passagesTested = row.getString(TSV.Metadata.passages_tested.name());
            String hostStrainFull = row.getString(TSV.Metadata.validation_host_strain_full.name());

            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODEL_KEY, modelId);
            if (modelCreation == null) throw new NullPointerException();

            QualityAssurance qa = new QualityAssurance();
            qa.setTechnology(validationTechnique);
            qa.setDescription(description);
            qa.setPassages(passagesTested);
            qa.setValidationHostStrain(hostStrainFull);
            modelCreation.addQualityAssurance(qa);
        }
    }

    void createSharingData() {
        log.info("Creating sharing data");
        Table sharingTable = pdxDataTables.get("metadata-sharing.tsv");

        Group providerGroup = (Group) domainObjects.get(PROVIDER_KEY).get(null);
        if (providerGroup == null) throw new NullPointerException();

        for (Row row : sharingTable) {
            String modelId = row.getString(TSV.Metadata.model_id.name());
            String providerType = row.getString(TSV.Metadata.provider_type.name());
            String accessibility = row.getString(TSV.Metadata.accessibility.name());
            String europdxAccessModality = row.getString(TSV.Metadata.europdx_access_modality.name());
            String email = row.getString(TSV.Metadata.email.name());
            String formUrl = row.getString(TSV.Metadata.form_url.name());
            String databaseUrl = row.getString(TSV.Metadata.database_url.name());
            String project = row.getString(TSV.Metadata.project.name());

            ModelCreation modelCreation = (ModelCreation) getDomainObject(MODEL_KEY, modelId);
            if (modelCreation == null) throw new NullPointerException();

            List<ExternalUrl> externalUrls = getExternalUrls(email, formUrl, databaseUrl);
            modelCreation.setExternalUrls(externalUrls);

            Optional.ofNullable(project).ifPresent(
                    s -> {
                        Group projectGroup = dataImportService.getProjectGroup(s);
                        modelCreation.addGroup(projectGroup);
                    });

            if (eitherIsPresent(accessibility, europdxAccessModality)) {
                Group access = dataImportService.getAccessibilityGroup(accessibility, europdxAccessModality);
                modelCreation.addGroup(access);
            }

            providerGroup.setProviderType(providerType);
            providerGroup.setContact(email);
        }
    }

    void createSamplePlatformData() {
        log.info("Creating sample platforms");
        Table samplePlatformTable = pdxDataTables.get("sampleplatform-data.tsv");

        if (samplePlatformTable == null) return;

        for (Row row : samplePlatformTable) {


            String sampleOrigin = row.getString(TSV.SamplePlatform.sample_origin.name());
            String platformName = row.getString(TSV.SamplePlatform.platform.name());
            String molCharType = row.getString(TSV.SamplePlatform.molecular_characterisation_type.name());

            Sample sample = null;

            if (sampleOrigin.equals(PATIENT_KEY)) {

                sample = getPatientSample(row);
            } else if (sampleOrigin.equals("xenograft")) {

                sample = getOrCreateSpecimen(row).getSample();
            }

            if (sample == null) throw new NullPointerException();


            getOrCreateMolecularCharacterization(sample, platformName, molCharType);

        }

    }

    void createTreatmentData(){
        log.info("Creating patient treatments");
        Table treatmentTable = pdxDataTables.get("patienttreatment-Sheet1.tsv");

        if(treatmentTable != null){

           for(Row row : treatmentTable){

                String patientId = getStringFromRowAndColumn(row, TSV.Metadata.patient_id.name());

                Patient patient = (Patient) getDomainObject(PATIENT_KEY, patientId);

                if(patient == null) throw new NullPointerException();

                PatientSnapshot patientSnapshot = patient.getLastSnapshot();

                TreatmentProtocol treatmentProtocol = getTreatmentProtocol(row);
                patientSnapshot.addTreatmentProtocol(treatmentProtocol);

           }
        }

    }

    void createDrugDosingData(){

        Table drugdosingTable = pdxDataTables.get("drugdosing-Sheet1.tsv");

        if(drugdosingTable != null){

            for(Row row : drugdosingTable){

                String modelId = getStringFromRowAndColumn(row, TSV.Metadata.model_id.name());

               ModelCreation model = (ModelCreation) getDomainObject(MODEL_KEY, modelId);

                if(model == null) throw new NullPointerException();

                TreatmentProtocol treatmentProtocol = getTreatmentProtocol(row);
                model.addTreatmentProtocol(treatmentProtocol);

            }
        }

    }

    void createMolecularData() {
        log.info("Creating molecular data");
        createMutationData();
        createCNAData();
        createCytogeneticsData();

    }

    private void createMutationData(){

        Table mutationTable = pdxDataTables.get("mut.tsv");

        if(mutationTable != null){

            createMolecularCharacterization(mutationTable, "mutation");
        }
        else{

            Map<String, Object> models = domainObjects.get(MODEL_KEY);

            for(Map.Entry<String, Object> entry : models.entrySet()){

                ModelCreation modelCreation = (ModelCreation) entry.getValue();
                String mutationModelId = "mut_"+modelCreation.getSourcePdxId()+".tsv";
                log.info(modelCreation.getSourcePdxId());
                mutationTable = pdxDataTables.get(mutationModelId);

                if(mutationTable != null){

                    createMolecularCharacterization(mutationTable, "mutation");
                }


            }

        }


    }

    private void createMolecularCharacterization(Table table, String molcharType){

        for (Row row : table) {

            MolecularCharacterization molecularCharacterization = getMolcharByType(row, molcharType);
            addMolecularData(molecularCharacterization, row);
        }
    }


    private void createCNAData(){

        Table cnaTable = pdxDataTables.get("cna.tsv");

        if(cnaTable != null){

            for (Row row : cnaTable) {

                MolecularCharacterization molecularCharacterization = getMolcharByType(row, "cna");
                addMolecularData(molecularCharacterization, row);
            }
        }
    }

    private void createCytogeneticsData(){

        Table cytoTable = pdxDataTables.get("cytogenetics-Sheet1.tsv");

        if(cytoTable != null){

            for (Row row : cytoTable) {

                MolecularCharacterization molecularCharacterization = getMolcharByType(row, "cytogenetics");
                addMolecularData(molecularCharacterization, row);
            }
        }

    }



    private MolecularCharacterization getMolcharByType(Row row, String molCharType) {

        String sampleOrigin = row.getString("sample_origin");
        String platformName = row.getString(PLATFORM_KEY);
        Sample sample = null;

        if (sampleOrigin.equalsIgnoreCase("patient")) {

            sample = getPatientSample(row);
        } else if (sampleOrigin.equalsIgnoreCase("xenograft")) {

            sample = getOrCreateSpecimen(row).getSample();
        }

        if (sample == null) {
            log.error(sampleOrigin);
            throw new NullPointerException();}

        return getOrCreateMolecularCharacterization(sample, platformName, molCharType);
    }


    private Sample getPatientSample(Row row) {

        String modelId = row.getString(TSV.Mutation.model_id.name());
        ModelCreation modelCreation = (ModelCreation) getDomainObject(MODEL_KEY, modelId);
        if (modelCreation == null) throw new NullPointerException();

        return modelCreation.getSample();
    }

    private Specimen getOrCreateSpecimen(Row row) {

        String modelId = row.getString(TSV.Mutation.model_id.name());
        String hostStrainSymbol = row.getString(TSV.Mutation.host_strain_nomenclature.name());
        String passage = getStringFromRowAndColumn(row, TSV.Mutation.passage.name());
        if(hostStrainSymbol.equals("")) hostStrainSymbol = NOT_SPECIFIED;

        String sampleId = row.getString(TSV.Mutation.sample_id.name());

        ModelCreation modelCreation = (ModelCreation) getDomainObject(MODEL_KEY, modelId);
        if (modelCreation == null){
            log.error("Model not found: {}", modelId);
            throw new NullPointerException();
        }

        Specimen specimen = modelCreation.getSpecimenByPassageAndHostStrain(passage, hostStrainSymbol);

        if (specimen == null) {
            specimen = new Specimen();
            specimen.setPassage(passage);

            HostStrain hostStrain = getOrCreateHostStrain(NOT_SPECIFIED, hostStrainSymbol, row.getRowNumber());
            specimen.setHostStrain(hostStrain);

            Sample sample = new Sample();
            sample.setSourceSampleId(sampleId);
            specimen.setSample(sample);
            modelCreation.addSpecimen(specimen);
            modelCreation.addRelatedSample(sample);
        }

        return specimen;
    }

    private MolecularCharacterization getOrCreateMolecularCharacterization(Sample sample, String platformName, String molCharType) {

        MolecularCharacterization molecularCharacterization = sample.getMolecularCharacterization(molCharType, platformName);

        if (molecularCharacterization == null) {

            molecularCharacterization = new MolecularCharacterization();
            molecularCharacterization.setType(molCharType);
            molecularCharacterization.setPlatform(getOrCreatePlatform(platformName, molCharType));
            MarkerAssociation markerAssociation = new MarkerAssociation();
            molecularCharacterization.addMarkerAssociation(markerAssociation);

            sample.addMolecularCharacterization(molecularCharacterization);

        }

        return molecularCharacterization;
    }


    private Platform getOrCreatePlatform(String platformName, String molCharType) {

        Group providerGroup = (Group) getDomainObject(PROVIDER_KEY, null);
        String platformId = molCharType + platformName;
        Platform platform = (Platform) getDomainObject(PLATFORM_KEY, platformId);

        if (platform == null) {

            platform = new Platform();
            platform.setGroup(providerGroup);
            platform.setName(platformName);

            addDomainObject(PLATFORM_KEY, platformId, platform);
        }

        return platform;
    }


    private void addMolecularData(MolecularCharacterization molecularCharacterization, Row row) {

        MarkerAssociation markerAssociation = molecularCharacterization.getMarkerAssociations().get(0);

        if (markerAssociation == null) {
            markerAssociation = new MarkerAssociation();
            molecularCharacterization.addMarkerAssociation(markerAssociation);
        }

        String hgncSymbol = row.getString("symbol");
        String modelId = row.getString("model_id");
        Group provider = (Group) domainObjects.get(PROVIDER_KEY).get(null);
        String dataSource = provider.getAbbreviation();

        NodeSuggestionDTO nsdto = dataImportService.getSuggestedMarker(this.getClass().getSimpleName(),
                dataSource,
                modelId,
                hgncSymbol,
                molecularCharacterization.getType(),
                molecularCharacterization.getPlatform().getName());

        Marker marker;

        if (nsdto.getNode() == null) {

            //log.error(nsdto.getLogEntity().getMessage());
        } else {


            // step 4: assemble the MarkerAssoc object and add it to molchar
            marker = (Marker) nsdto.getNode();

            //if we have any message regarding the suggested marker, ie: prev symbol, synonym, etc, add it to the report
            if (nsdto.getLogEntity() != null) {
                log.info(nsdto.getLogEntity().getMessage());
            }

            MolecularData molecularData = null;

            if (molecularCharacterization.getType().equals("mutation")) {

                molecularData = getMutationProperties(row, marker);
            }
            else if (molecularCharacterization.getType().equals("cytogenetics")) {

                molecularData = getCytogeneticsProperties(row, marker);
            }
            else if(molecularCharacterization.getType().equals("copynumberalteration")){
                molecularData = getCNAProperties(row, marker);
            }

            markerAssociation.addMolecularData(molecularData);

        }
    }

    private MolecularData getMutationProperties(Row row, Marker marker) {

        MolecularData ma = new MolecularData();
        try {
            ma.setAminoAcidChange(getStringFromRowAndColumn(row, TSV.Mutation.amino_acid_change.name()));
            ma.setConsequence(getStringFromRowAndColumn(row, TSV.Mutation.consequence.name()));
            ma.setAlleleFrequency(getStringFromRowAndColumn(row, TSV.Mutation.allele_frequency.name()));
            ma.setChromosome(getStringFromRowAndColumn(row, TSV.Mutation.chromosome.name()));
            ma.setReadDepth(getStringFromRowAndColumn(row, TSV.Mutation.read_depth.name()));
            ma.setRefAllele(getStringFromRowAndColumn(row, TSV.Mutation.ref_allele.name()));
            ma.setAltAllele(getStringFromRowAndColumn(row, TSV.Mutation.alt_allele.name()));
            ma.setGenomeAssembly(getStringFromRowAndColumn(row, TSV.Mutation.genome_assembly.name()));
            ma.setRsIdVariants(getStringFromRowAndColumn(row, TSV.Mutation.variation_id.name()));
            ma.setSeqStartPosition(getStringFromRowAndColumn(row, TSV.Mutation.seq_start_position.name()));

            ma.setEnsemblTranscriptId(getStringFromRowAndColumn(row, TSV.Mutation.ensembl_transcript_id.name()));
            ma.setNucleotideChange("");
            ma.setMarker(marker.getHgncSymbol());
        } catch (Exception e) {
            log.error(e.toString());
        }
        return ma;
    }


    private MolecularData getCNAProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();

        ma.setChromosome(row.getString(TSV.CopyNumberAlteration.chromosome.name()));
        ma.setSeqStartPosition(row.getString(TSV.CopyNumberAlteration.seq_start_position.name()));
        ma.setSeqEndPosition(row.getString(TSV.CopyNumberAlteration.seq_end_position.name()));
        ma.setCnaLog10RCNA(row.getString(TSV.CopyNumberAlteration.log10r_cna.name()));
        ma.setCnaLog2RCNA(row.getString(TSV.CopyNumberAlteration.log2r_cna.name()));
        ma.setCnaCopyNumberStatus(row.getString(TSV.CopyNumberAlteration.copy_number_status.name()));
        ma.setCnaGisticValue(row.getString(TSV.CopyNumberAlteration.gistic_value.name()));
        ma.setCnaPicnicValue(row.getString(TSV.CopyNumberAlteration.picnic_value.name()));
        ma.setGenomeAssembly(row.getString(TSV.CopyNumberAlteration.genome_assembly.name()));

        ma.setMarker(marker.getHgncSymbol());
        return  ma;
    }

    private MolecularData getTranscriptomicProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();
        ma.setChromosome("");
        ma.setSeqStartPosition("");
        ma.setSeqEndPosition("");
        ma.setRnaSeqCoverage("");
        ma.setRnaSeqFPKM("");
        ma.setRnaSeqTPM("");
        ma.setRnaSeqCount("");
        ma.setAffyHGEAProbeId("");
        ma.setAffyHGEAExpressionValue("");
        ma.setIlluminaHGEAProbeId("");
        ma.setIlluminaHGEAExpressionValue("");
        ma.setGenomeAssembly("");
        ma.setZscore("");

        ma.setMarker(marker.getHgncSymbol());
        return  ma;
    }


    private MolecularData getCytogeneticsProperties(Row row, Marker marker){

        MolecularData ma = new MolecularData();
        try {

            ma.setMarker(marker.getHgncSymbol());
        } catch (Exception e) {

        }
        return ma;
    }


    private boolean eitherIsPresent(String string, String anotherString) {
        return (
                Optional.ofNullable(string).isPresent() ||
                        Optional.ofNullable(anotherString).isPresent()
        );
    }

    private List<ExternalUrl> getExternalUrls(String email, String formUrl, String databaseUrl) {
        List<ExternalUrl> externalUrls = new ArrayList<>();
        Optional.ofNullable(email).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.CONTACT, s)));
        Optional.ofNullable(formUrl).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.CONTACT, s)));
        Optional.ofNullable(databaseUrl).ifPresent(
                s -> externalUrls.add(
                        dataImportService.getExternalUrl(ExternalUrl.Type.SOURCE, s)));
        return externalUrls;
    }

    private Specimen createSpecimen(Row row, int rowNumber) {

        String hostStrainName = row.getString(TSV.Metadata.host_strain.name());
        String hostStrainNomenclature = row.getString(TSV.Metadata.host_strain_full.name());
        String engraftmentSiteName = row.getString(TSV.Metadata.engraftment_site.name());
        String engraftmentTypeName = row.getString(TSV.Metadata.engraftment_type.name());
        String sampleType = row.getString(TSV.Metadata.sample_type.name());
        String passageNum = row.getString(TSV.Metadata.passage_number.name());

        HostStrain hostStrain = getOrCreateHostStrain(hostStrainName, hostStrainNomenclature, rowNumber);
        EngraftmentSite engraftmentSite = getOrCreateEngraftment(engraftmentSiteName);
        EngraftmentType engraftmentType = getOrCreateEngraftmentType(engraftmentTypeName);
        EngraftmentMaterial engraftmentMaterial = getOrCreateEngraftmentMaterial(sampleType);

        Sample xenoSample = new Sample();
        Specimen specimen = new Specimen();
        specimen.setPassage(passageNum);
        specimen.setHostStrain(hostStrain);
        specimen.setEngraftmentMaterial(engraftmentMaterial);
        specimen.setEngraftmentSite(engraftmentSite);
        specimen.setEngraftmentType(engraftmentType);
        specimen.setSample(xenoSample);

        return specimen;
    }

    private EngraftmentMaterial getOrCreateEngraftmentMaterial(String sampleType) {
        EngraftmentMaterial engraftmentMaterial = (EngraftmentMaterial) getDomainObject(ENGRAFTMENT_MATERIAL_KEY, sampleType);
        if (engraftmentMaterial == null) {
            engraftmentMaterial = dataImportService.getEngraftmentMaterial(sampleType);
            addDomainObject(ENGRAFTMENT_MATERIAL_KEY, sampleType, engraftmentMaterial);
        }
        return engraftmentMaterial;
    }

    private EngraftmentType getOrCreateEngraftmentType(String engraftmentTypeName) {
        EngraftmentType engraftmentType = (EngraftmentType) getDomainObject(ENGRAFTMENT_TYPE_KEY, engraftmentTypeName);
        if (engraftmentType == null) {
            engraftmentType = dataImportService.getImplantationType(engraftmentTypeName);
            addDomainObject(ENGRAFTMENT_TYPE_KEY, engraftmentTypeName, engraftmentType);
        }
        return engraftmentType;
    }

    private EngraftmentSite getOrCreateEngraftment(String engraftmentSiteName) {
        EngraftmentSite engraftmentSite = (EngraftmentSite) getDomainObject(ENGRAFTMENT_SITE_KEY, engraftmentSiteName);
        if (engraftmentSite == null) {
            engraftmentSite = dataImportService.getImplantationSite(engraftmentSiteName);
            addDomainObject(ENGRAFTMENT_SITE_KEY, engraftmentSiteName, engraftmentSite);
        }
        return engraftmentSite;
    }

    private HostStrain getOrCreateHostStrain(String hostStrainName, String hostStrainNomenclature, int rowNumber) {
        HostStrain hostStrain = (HostStrain) getDomainObject(HOST_STRAIN_KEY, hostStrainNomenclature);
        if (hostStrain == null) {
            try {
                hostStrain = dataImportService.getHostStrain(hostStrainName, hostStrainNomenclature, "", "");
                addDomainObject(HOST_STRAIN_KEY, hostStrainNomenclature, hostStrain);
            } catch (Exception e) {
                //log.error("Host strain symbol is empty in row {}", rowNumber);
            }
        }
        return hostStrain;
    }

    private Sample createPatientSample(Row row) {

        String diagnosis = row.getString(TSV.Metadata.diagnosis.name());
        String sampleId = row.getString(TSV.Metadata.sample_id.name());
        String tumorTypeName = row.getString(TSV.Metadata.tumour_type.name());
        String primarySiteName = row.getString(TSV.Metadata.primary_site.name());
        String collectionSiteName = row.getString(TSV.Metadata.collection_site.name());
        String stage = row.getString(TSV.Metadata.stage.name());
        String stagingSystem = row.getString(TSV.Metadata.staging_system.name());
        String grade = row.getString(TSV.Metadata.grade.name());
        String gradingSystem = row.getString(TSV.Metadata.grading_system.name());

        Tissue primarySite = getOrCreateTissue(primarySiteName);
        Tissue collectionSite = getOrCreateTissue(collectionSiteName);
        TumorType tumorType = getOrCreateTumorType(tumorTypeName);

        Sample sample = new Sample();
        sample.setType(tumorType);
        sample.setSampleSite(collectionSite);
        sample.setOriginTissue(primarySite);

        sample.setSourceSampleId(sampleId);
        sample.setDiagnosis(diagnosis);
        sample.setStage(stage);
        sample.setStageClassification(stagingSystem);
        sample.setGrade(grade);
        sample.setGradeClassification(gradingSystem);

        return sample;
    }

    private TumorType getOrCreateTumorType(String tumorTypeName) {
        TumorType tumorType = (TumorType) getDomainObject(TUMOR_TYPE_KEY, tumorTypeName);
        if (tumorType == null) {
            tumorType = dataImportService.getTumorType(tumorTypeName);
            addDomainObject(TUMOR_TYPE_KEY, tumorTypeName, tumorType);
        }
        return tumorType;
    }

    private Tissue getOrCreateTissue(String siteName) {
        Tissue primarySite = (Tissue) getDomainObject(TISSUE_KEY, siteName);
        if (primarySite == null) {
            primarySite = dataImportService.getTissue(siteName);
            addDomainObject(TISSUE_KEY, siteName, primarySite);
        }
        return primarySite;
    }

    private void persistNodes() {

        persistPatients();
        persistModels();

    }

    public void persistPatients(){

        log.info("Persisiting patients");
        Map<String, Object> patients = domainObjects.get(PATIENT_KEY);
        for (Object pat : patients.values()) {

            Patient patient = (Patient) pat;

            Set<PatientSnapshot>  snapshots = patient.getSnapshots();
            for(PatientSnapshot ps: snapshots){
                Set<Sample> patientSamples = ps.getSamples();

                for(Sample patientSample : patientSamples){
                    convertMarkerAssociations(patientSample);
                }
            }

            dataImportService.savePatient(patient);
        }
    }

    public void persistModels(){

        log.info("Persisiting models");
        Map<String, Object> models = domainObjects.get(MODEL_KEY);
        for (Object mod : models.values()) {
            ModelCreation model = (ModelCreation) mod;

            if(model.getSpecimens() != null){

                Set<Specimen> specimenSet = model.getSpecimens();
                for(Specimen specimen:specimenSet){

                    convertMarkerAssociations(specimen.getSample());
                }
            }

            dataImportService.saveModelCreation(model);
        }
    }


    public void addDomainObject(String key1, String key2, Object object) {

        if (domainObjects.containsKey(key1)) {
            domainObjects.get(key1).put(key2, object);
        } else {
            Map map = new HashMap();
            map.put(key2, object);
            domainObjects.put(key1, map);
        }
    }

    public Object getDomainObject(String key1, String key2) {

        if (containsBothKeys(key1, key2)) {
            return domainObjects.get(key1).get(key2);
        }
        return null;
    }

    private boolean containsBothKeys(String key1, String key2) {
        return domainObjects.containsKey(key1) && domainObjects.get(key1).containsKey(key2);
    }


    private void convertMarkerAssociations(Sample sample) {

        if (sample.getMolecularCharacterizations() != null) {

            for (MolecularCharacterization molecularCharacterization : sample.getMolecularCharacterizations()) {
                if(molecularCharacterization.getMarkerAssociations() != null){
                    molecularCharacterization.getMarkerAssociations().get(0).createMolecularDataStringFromList();
                }

            }
        }
    }



    private TreatmentProtocol getTreatmentProtocol(Row row){

        String drugString = "";
        String doseString = "";
        String response = "";
        String responseClassification = "";

        String[] drugArray = drugString.split("\\+");
        String[] doseArray = doseString.split(";");

        TreatmentProtocol tp = new TreatmentProtocol();
        if(drugArray.length == doseArray.length){

            for(int i=0;i<drugArray.length;i++){

                Treatment treatment = new Treatment();
                treatment.setName(drugArray[i].trim());
                TreatmentComponent tc = new TreatmentComponent();
                tc.setDose(doseArray[i].trim());

                tc.setTreatment(treatment);
                tp.addTreatmentComponent(tc);
            }
            return tp;
        }

        else if(drugArray.length != doseArray.length && doseArray.length == 1){

            for(int i=0;i<drugArray.length;i++){

                Treatment treatment = new Treatment();
                treatment.setName(drugArray[i].trim());

                TreatmentComponent tc = new TreatmentComponent();
                tc.setDose(doseArray[0].trim());

                tc.setTreatment(treatment);
                tp.addTreatmentComponent(tc);

            }
            return tp;
        }

        return null;
    }


    private String getStringFromRowAndColumn(Row row, String columnName){

        try {
            if (row.getColumnType(columnName) == ColumnType.STRING) {
                return row.getString(columnName);
            } else if (row.getColumnType(columnName) == ColumnType.DOUBLE) {

                return Double.toString(row.getDouble(columnName));
            } else if (row.getColumnType(columnName) == ColumnType.INTEGER) {

                return Integer.toString(row.getInt(columnName));
            }

        }
        catch(Exception e){
           //column is not present, so return null
           return null;
        }
        return null;
    }


    private String getMolecularTypeKey(String s){

        return "";
    }

}