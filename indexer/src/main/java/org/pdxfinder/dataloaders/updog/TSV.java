package org.pdxfinder.dataloaders.updog;

public class TSV {

    public enum Metadata {
        patient_id,
        model_id,
        sample_id,
        provider_group,
        sex,
        ethnicity,
        history,
        initial_diagnosis,
        age_at_initial_diagnosis,
        name,
        abbreviation,
        internal_url,
        collection_date,
        age_in_years_at_collection,
        collection_event,
        months_since_collection_1,
        diagnosis,
        tumour_type,
        primary_site,
        collection_site,
        stage,
        staging_system,
        grade,
        grading_system,
        virology_status,
        sharable,
        treatment_naive_at_collection,
        treated,
        prior_treatment,
        host_strain,
        host_strain_full,
        engraftment_site,
        engraftment_type,
        sample_type,
        sample_state,
        passage_number,
        publications,
        validation_technique,
        description,
        passages_tested,
        validation_host_strain_full,
        provider_name,
        provider_abbreviation,
        project,
        provider_type,
        accessibility,
        europdx_access_modality,
        email,
        form_url,
        database_url
    }

    public enum Mutation{
        model_id,
        sample_id,
        sample_origin,
        host_strain_nomenclature,
        passage,
        symbol,
        biotype,
        coding_sequence_change,
        variant_class,
        codon_change,
        amino_acid_change,
        consequence,
        functional_prediction,
        read_depth,
        allele_frequency,
        chromosome,
        seq_start_position,
        ref_allele,
        alt_allele,
        ucsc_gene_id,
        ncbi_gene_id,
        ncbi_transcript_id,
        ensembl_gene_id,
        ensembl_transcript_id,
        variation_id,
        genome_assembly,
        platform
    }


    public enum SamplePlatform{

        sample_id,
        sample_origin,
        passage,
        engrafted_tumor_collection_site,
        model_id,
        host_strain_name,
        host_strain_nomenclature,
        molecular_characterisation_type,
        platform,
        internal_protocol_url

    }

}