package org.pdxfinder.services.ds;

import org.pdxfinder.dao.ModelCreation;
import org.pdxfinder.repositories.ModelCreationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/*
 * Created by csaba on 19/01/2018.
 */

@Component
public class SearchDS {

    private final static Logger log = LoggerFactory.getLogger(SearchDS.class);

    private Set<ModelForQuery> models;

    /**
     * Populate the complete set of models for searching when this object is instantiated
     */
    public SearchDS(ModelCreationRepository modelCreationRepository) {
        Assert.notNull(modelCreationRepository, "Model repository cannot be null");
        this.models = new HashSet<>();

        // When this class is instantiated, populate and cache the models set

        for (ModelCreation mc : modelCreationRepository.getModelsWithPatientData()) {

            ModelForQuery mfq = new ModelForQuery();
            mfq.setModelId(mc.getId());
            mfq.setDatasource(mc.getDataSource());

            // Patient information
            mfq.setPatientAge(mc.getSample().getPatientSnapshot().getAgeBin());
            mfq.setPatientGender(mc.getSample().getPatientSnapshot().getPatient().getSex());

            if (mc.getSample().getPatientSnapshot().getTreatmentNaive() != null) {
                mfq.setPatientTreatmentStatus(mc.getSample().getPatientSnapshot().getTreatmentNaive().toString());
            }

            mfq.setSampleExtractionMethod(mc.getSample().getExtractionMethod());
            mfq.setSampleOriginTissue(mc.getSample().getOriginTissue().getName());
            mfq.setSampleClassification(mc.getSample().getClassification());
            // etc.

            models.add(mfq);
        }
    }


    public Set<ModelForQuery> getModels() {
        return models;
    }

    public void setModels(Set<ModelForQuery> models) {
        this.models = models;
    }


    /**
     * Search function accespts a Map of key value pairs
     * key = what facet to search
     * list of values = what values to filter on (using OR)
     * <p>
     * EX of expected data structure:
     * <p>
     * patient_age -> { 5-10, 20-40 },
     * patient_gender -> { Male },
     * sample_origin_tissue -> { Lung, Liver }
     * <p>
     * would yield results for male patients between 5-10 OR between 20-40 AND that had cancers in the lung OR liver
     *
     * @param filters
     * @return set of models derived from filtering the complete set according to the
     * filters passed in as arguments
     */
    public Set<ModelForQuery> search(Map<SearchFacetName, List<String>> filters) {

        Set<ModelForQuery> result = new HashSet<>(models);

        // If no filters have been specified, return the complete set
        if (filters == null) {
            return result;
        }

        for (SearchFacetName facet : filters.keySet()) {
            List<Predicate<ModelForQuery>> preds = new ArrayList<>();
            Predicate predicate;
            switch (facet) {

                case datasource:

                    preds.clear();
                    for (String filter : filters.get(SearchFacetName.datasource)) {
                        Predicate<ModelForQuery> p = s -> s.getDatasource().equals(filter);
                        preds.add(p);
                    }
                    predicate = preds.stream().reduce(Predicate::or).orElse(x -> false);

                    result = result.stream().filter(predicate::test).collect(Collectors.toSet());
                    break;

                case patient_age:

                    preds.clear();
                    for (String filter : filters.get(SearchFacetName.patient_age)) {
                        Predicate<ModelForQuery> p = s -> s.getPatientAge().equals(filter);
                        preds.add(p);
                    }
                    predicate = preds.stream().reduce(Predicate::or).orElse(x -> false);

                    result = result.stream().filter(predicate::test).collect(Collectors.toSet());
                    break;

                case patient_treatment_status:

                    predicate = getExactMatchDisjunctionPredicate(filters.get(SearchFacetName.patient_treatment_status));
                    result = result.stream().filter(predicate::test).collect(Collectors.toSet());
                    break;

                case patient_gender:

                    predicate = getExactMatchDisjunctionPredicate(filters.get(SearchFacetName.patient_gender));
                    result = result.stream().filter(predicate::test).collect(Collectors.toSet());
                    break;

                case sample_origin_tissue:

                    preds.clear();
                    for (String filter : filters.get(SearchFacetName.sample_origin_tissue)) {
                        Predicate<ModelForQuery> p = s -> s.getSampleOriginTissue().equals(filter);
                        preds.add(p);
                    }
                    predicate = preds.stream().reduce(Predicate::or).orElse(x -> false);

                    result = result.stream().filter(predicate::test).collect(Collectors.toSet());
                    break;

                case sample_sample_site:
                    //TODO: Add this section
                    break;

                case sample_extraction_method:
                    //TODO: Add this section
                    break;

                case sample_classification:
                    //TODO: Add this section
                    break;

                case sample_tumor_type:
                    //TODO: Add this section
                    break;

                case model_implantation_site:
                    //TODO: Add this section
                    break;

                case model_implantation_type:
                    //TODO: Add this section
                    break;

                case model_background_strain:
                    //TODO: Add this section
                    break;

                case system:
                    //TODO
                    break;

                case organ:
                    //TODO
                    break;

                case cell_type:
                    //TODO
                    break;

                default:
                    // default case is an unexpected filter option
                    // Do not filter anything
                    log.info("Unrecognised facet {} passed to search, skipping.", facet);
                    break;
            }
        }

        return result;
    }

    /**
     * getExactMatchDisjunctionPredicate returns a composed predicate with all the supplied filters "OR"ed together
     * using an exact match
     * <p>
     * NOTE: This is a case sensitive match!
     *
     * @param filters the set of strings to match against
     * @return a composed predicate case insensitive matching the supplied filters using disjunction (OR)
     */
    Predicate<ModelForQuery> getExactMatchDisjunctionPredicate(List<String> filters) {
        List<Predicate<ModelForQuery>> preds = new ArrayList<>();

        // Iterate through the filter options passed in for this facet
        for (String filter : filters) {

            // Create a filter predicate for each option
            Predicate<ModelForQuery> pred = s -> s.getDatasource().equals(filter);

            // Store all filter options in a list
            preds.add(pred);
        }

        // Create a "combination" predicate containing sub-predicates "OR"ed together
        return preds.stream().reduce(Predicate::or).orElse(x -> false);
    }

    /**
     * getContainsMatchDisjunctionPredicate returns a composed predicate with all the supplied filters "OR"ed together
     * using a contains match
     * <p>
     * NOTE: This is a case insensitive match!
     *
     * @param filters the set of strings to match against
     * @return a composed predicate case insensitive matching the supplied filters using disjunction (OR)
     */
    Predicate getContainsMatchDisjunctionPredicate(List<String> filters) {
        List<Predicate<String>> preds = new ArrayList<>();

        // Iterate through the filter options passed in for this facet
        for (String filter : filters) {

            // Create a filter predicate for each option
            Predicate<String> pred = s -> s.toLowerCase().contains(filter.toLowerCase());

            // Store all filter options in a list
            preds.add(pred);
        }

        // Create a "combination" predicate containing sub-predicates "OR"ed together
        return preds.stream().reduce(Predicate::or).orElse(x -> false);
    }


}
