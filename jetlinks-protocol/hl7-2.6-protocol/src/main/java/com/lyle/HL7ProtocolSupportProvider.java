package com.lyle;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.MetadataFeature;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.official.JetLinksDeviceMetadata;
import org.jetlinks.supports.official.JetLinksPropertyMetadata;
import reactor.core.publisher.Mono;

public class HL7ProtocolSupportProvider implements ProtocolSupportProvider {

    private JetLinksDeviceMetadata createDeviceMetadata() {
        JetLinksDeviceMetadata metadata = new JetLinksDeviceMetadata("protocol", "迈瑞协议包物模型HL7_2.6");

        // ECG Parameters (Table 15)
        metadata.addProperty(new JetLinksPropertyMetadata("147842_MDC_ECG_HEART_RATE", "ECG Heart Rate", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("151578_MDC_TTHOR_RESP_RATE", "Transthoracic Respiration Rate", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("148066_MDC_ECG_V_P_C_RATE", "PVCs/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("108_MNDRY_ECG_PAUSE_RATE", "Pauses/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("352_MNDRY_ECG_VPB_RATE", "VPBs/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("298_MNDRY_ECG_COUPLETS_RATE", "Couplets/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("299_MNDRY_ECG_MISSED_BEATS_RATE", "Missed Beats/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("300_MNDRY_ECG_PACING_NON_CAPT_RATE", "PNCs/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("301_MNDRY_ECG_PACER_NOT_PACING_RATE", "PNPs/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("302_MNDRY_ECG_P_V_C_RonT_RATE", "RonTs/min", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147232_MDC_ECG_TIME_PD_QT_GL", "Global QT-Interval Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("303_MNDRY_ECG_TIME_PD_QT_GL_REF", "Global QT-Interval Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("304_MNDRY_ECG_QTC_GL", "Global QTC-Interval", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("305_MNDRY_ECG_QTC_GL_REF", "Global QTC-Interval Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("307_MNDRY_ECG_QTC_HR", "QT HR Current", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("308_MNDRY_ECG_QTC_HR_REF", "QT HR Reference", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("309_MNDRY_ECG_QTC_DIFF", "AQTC", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147216_MDC_ECG_TIME_PD_PQ", "PR interval", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147228_MDC_ECG_TIME_PD_QRS_GL", "QRS Duration", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147200_MDC_ECG_ANGLE_P_FRONT", "P-Axis", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147204_MDC_ECG_ANGLE_QRS_FRONT", "QRS-Axis", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("147208_MDC_ECG_ANGLE_T_FRONT", "T-Axis", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("133127_MDC_ECG_AMPL_R_V5", "R-Wave Amplitude, V5", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("133379_MDC_ECG_AMPL_S_V1", "S-Wave Amplitude, V1", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131841_MDC_ECG_AMPL_ST_I", "ST I Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("72_MNDRY_ECG_TEMP_AMPL_ST_I", "ST I Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("73_MNDRY_ECG_REF_AMPL_ST_I", "ST I Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131842_MDC_ECG_AMPL_ST_II", "ST II Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("74_MNDRY_ECG_TEMP_AMPL_ST_II", "ST II Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("75_MNDRY_ECG_REF_AMPL_ST_II", "ST II Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131901_MDC_ECG_AMPL_ST_III", "ST III Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("76_MNDRY_ECG_TEMP_AMPL_ST_III", "ST III Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("77_MNDRY_ECG_REF_AMPL_ST_III", "ST III Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131902_MDC_ECG_AMPL_ST_AVR", "ST AVR Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("78_MNDRY_ECG_TEMP_AMPL_ST_AVR", "ST aVR Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("79_MNDRY_ECG_REF_AMPL_ST_AVR", "ST AVR Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131903_MDC_ECG_AMPL_ST_AVL", "ST AVL Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("80_MNDRY_ECG_TEMP_AMPL_ST_AVL", "ST aVL Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("81_MNDRY_ECG_REF_AMPL_ST_AVL", "ST AVL Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131904_MDC_ECG_AMPL_ST_AVF", "ST AVF Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("82_MNDRY_ECG_TEMP_AMPL_ST_AVF", "ST aVF Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("83_MNDRY_ECG_REF_AMPL_ST_AVF", "ST AVF Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131843_MDC_ECG_AMPL_ST_V1", "ST V1 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("84_MNDRY_ECG_TEMP_AMPL_ST_V1", "ST V1 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("85_MNDRY_ECG_REF_AMPL_ST_V1", "ST V1 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131844_MDC_ECG_AMPL_ST_V2", "ST V2 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("86_MNDRY_ECG_TEMP_AMPL_ST_V2", "ST V2 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("87_MNDRY_ECG_REF_AMPL_ST_V2", "ST V2 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131845_MDC_ECG_AMPL_ST_V3", "ST V3 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("88_MNDRY_ECG_TEMP_AMPL_ST_V3", "ST V3 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("89_MNDRY_ECG_REF_AMPL_ST_V3", "ST V3 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131846_MDC_ECG_AMPL_ST_V4", "ST V4 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("90_MNDRY_ECG_TEMP_AMPL_ST_V4", "ST V4 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("91_MNDRY_ECG_REF_AMPL_ST_V4", "ST V4 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131847_MDC_ECG_AMPL_ST_V5", "ST V5 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("92_MNDRY_ECG_TEMP_AMPL_ST_V5", "ST V5 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("93_MNDRY_ECG_REF_AMPL_ST_V5", "ST V5 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131848_MDC_ECG_AMPL_ST_V6", "ST V6 Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("94_MNDRY_ECG_TEMP_AMPL_ST_V6", "ST V6 Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("95_MNDRY_ECG_REF_AMPL_ST_V6", "ST V6 Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("131927_MDC_ECG_AMPL_ST_V", "ST V/Vx Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("96_MNDRY_ECG_TEMP_AMPL_ST_V", "ST V/Vx Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("97_MNDRY_ECG_REF_AMPL_ST_V", "ST V/Vx Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("106_MNDRY_ECG_AMPL_ST_VB", "ST Vy Current", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("98_MNDRY_ECG_TEMP_AMPL_ST_VB", "ST Vy Template", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("107_MNDRY_ECG_REF_AMPL_ST_VB", "ST Vy Reference", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("392_MNDRY_ECG_AMPL_DELTA_ST_I", "DELTA_ST_I", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("393_MNDRY_ECG_AMPL_DELTA_ST_II", "DELTA_ST_II", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("394_MNDRY_ECG_AMPL_DELTA_ST_III", "DELTA_ST_III", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("395_MNDRY_ECG_AMPL_DELTA_ST_AVR", "DELTA_ST_AVF", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("396_MNDRY_ECG_AMPL_DELTA_ST_AVL", "DELTA_ST_AVL", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("397_MNDRY_ECG_AMPL_DELTA_ST_AVF", "DELTA ST AVR", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("398_MNDRY_ECG_AMPL_DELTA_ST_V1", "DELTA STV1", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("399_MNDRY_ECG_AMPL_DELTA_ST_V2", "DELTA_ST_V2", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("400_MNDRY_ECG_AMPL_DELTA_ST_V3", "DELTA_ST_V3", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("401_MNDRY_ECG_AMPL_DELTA_ST_V4", "DELTA_ST_V4", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("402_MNDRY_ECG_AMPL_DELTA_ST_V5", "DELTA_ST_V5", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("403_MNDRY_ECG_AMPL_DELTA_ST_V6", "DELTA_ST_V6", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("404_MNDRY_ECG_AMPL_DELTA_ST_V", "DELTA_ST_Va", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("405_MNDRY_ECG_AMPL_DELTA_ST_VB", "DELTA_ST_Vb", DoubleType.GLOBAL));

        // Pulse Oximetry (Table 16)
        metadata.addProperty(new JetLinksPropertyMetadata("150456_MDC_PULS_OXIM_SAT_O2", "SpO2 1 Saturation", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("149530_MDC_PULS_OXIM_PULS_RATE", "SpO2 1 Pulse Rate", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150488_MDC_BLD_PERF_INDEX", "SpO2 1 Perfusion Index", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("137_MNDRY_PULS_OXIM_SAT_02_DIFF", "▲SpO2", DoubleType.GLOBAL));

        // Venous Saturation (Table 17)
        metadata.addProperty(new JetLinksPropertyMetadata("150332_MDC_SAT_02_VEN", "SvO2", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("109_MNDRY_SAT_O2_VEN_CENT", "ScvO2(Central Venous Oxygen Saturation)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150324_MDC_SAT_02_ART", "SaO2", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152420_MDC_FLOW_02_CONSUMP", "VO2", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("138_MNDRY_SAT_02_DELIV", "DO2", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("139_MNDRY_SAT_02_DELIV_INDEX", "DO2I", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("140_MNDRY_SAT_02_CONSUMP_INDEX", "VO2I", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("159764_MDC_CONC_HB_ART", "Hb", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("160132_MDC_CONC_HCT_GEN", "Hct", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("141_MNDRY_SAT_O2_EXTRACTION_INDEX", "O2EI", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("142_MNDRY_SAT_02_SIGNAL_QUALITY_INDEX_EX", "SvO2 SQI", DoubleType.GLOBAL));

        // Pressure Calculations (Table 18)
        metadata.addProperty(new JetLinksPropertyMetadata("153604_MDC_PRESS_CEREB_PERF", "CePP", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("104_MNDRY_PRESS_ABDOM_PERF", "APP", DoubleType.GLOBAL));

        // Blood Pressure (Table 19)
        metadata.addProperty(new JetLinksPropertyMetadata("150017_MDC_PRESS_BLD_SYS", "IBP Systolic, Channel 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150018_MDC_PRESS_BLD_DIA", "IBP Diastolic, Channel 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150019_MDC_PRESS_BLD_MEAN", "IBP Mean, Channel 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150037_MDC_PRESS_BLD_ART_ABP_SYS", "ART Systolic, Channel", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150038_MDC_PRESS_BLD_ART_ABP_DIA", "ART Diastolic, Channel", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150039_MDC_PRESS_BLD_ART_ABP_MEAN", "ART Mean, Channel", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("364_MNDRY_BLD_PULS_RATE_ART_ABP", "ART pulse rate", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150301_MDC_PRESS_CUFF_SYS", "NIBP Systolic", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150302_MDC_PRESS_CUFF_DIA", "NIBP Diastolic", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150303_MDC_PRESS_CUFF_MEAN", "NIBP Mean", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("149546_MDC_PULS_RATE_NON_INV", "NIBP Pulse Rate", IntType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150300_MDC_PRESS_CUFF", "NIBP Cuff Pressure", DoubleType.GLOBAL));

        // Temperature (Table 20)
        metadata.addProperty(new JetLinksPropertyMetadata("150344_MDC_TEMP", "Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150372_MDC_TEMP_ESOPH", "Esophageal Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150380_MDC_TEMP_NASOPH", "Nasal Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188420_MDC_TEMP_RECT", "Rectal Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150348_MDC_TEMP_FOLEY", "Bladder Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("105_MNDRY_TEMP_AXIL", "Axillary Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150388_MDC_TEMP_SKIN", "Skin Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188424_MDC_TEMP_ORAL", "Oral Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150392_MDC_TEMP_TYMP", "Tympanic Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("112_MNDRY_TEMP_INTR_CRAN", "Intra-cranial Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("471_MNDRY_TEMP_TEMPLE", "Temple Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188428_MDC_TEMP_EAR", "Ear Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188436_MDC_TEMP_BLD", "Blood Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150368_MDC_TEMP_CORE", "Core Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188508_MDC_TEMP_ROOM", "Ambiance Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150356_MDC_TEMP_AWAY", "Airway Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188500_MDC_TEMP_MYO", "Myocardium Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150352_MDC_TEMP_ART", "Artery Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("150396_MDC_TEMP_VEN", "Vein Temperature 1-8", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188440_MDC_TEMP_DIFF", "▲Temperature 1-4", DoubleType.GLOBAL));

        // CO2 (Table 21)
        metadata.addProperty(new JetLinksPropertyMetadata("151716_MDC_CONC_AWAY_CO2_INSP", "Inspired CO2 (FiCO2)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("151708_MDC_CONC_AWAY_CO2_ET", "End-Tidal CO2 (EtCO2)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("151594_MDC_CO2_RESP_RATE", "CO2 Respiration Rate", IntType.GLOBAL));

        // Airway Gas Analyzer (Table 22)
        metadata.addProperty(new JetLinksPropertyMetadata("152196_MDC_CONC_AWAY_O2_INSP", "Inspired O2 (FiO2)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152440_MDC_CONC_AWAY_O2_ET", "End-Tidal O2 (EtO2)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152192_MDC_CONC_AWAY_N2O_INSP", "Inspired N2O (FiN2O)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152108_MDC_CONC_AWAY_N2O_ET", "End-Tidal N2O (EtN2O)", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152464_MDC_CONC_AWAY_AGENT_INSP", "Agent, Inspired (FIAA), Primary", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("152460_MDC_CONC_AWAY_AGENT_ET", "Agent, End Tidal (EtAA), Primary", DoubleType.GLOBAL));

        // Body Measurement (Table 23)
        metadata.addProperty(new JetLinksPropertyMetadata("188740_MDC_LEN_BODY_ACTUAL", "Height", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("188736_MDC_MASS_BODY_ACTUAL", "Weight", DoubleType.GLOBAL));

        // Wedge (Table 24)
        metadata.addProperty(new JetLinksPropertyMetadata("150052_MDC_PRESS_BLD_ART_PULM_OCCL", "PAWP", DoubleType.GLOBAL));

        // BIS (Table 27)
        metadata.addProperty(new JetLinksPropertyMetadata("120_MNDRY_EEG_BISPECTRAL_INDEX", "BIS", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("122_MNDRY_EEG_SIGNAL_QUALITY_INDEX", "SQI", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("153916_MDC_EMG_ELEC_POTL_MUSCL", "EMG", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("155024_MDC_EEG_PAROX_CRTX_BURST_SUPPRN", "SR", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("153992_MDC_EEG_FREQ_PWR_SPEC_CRTX_SPECTRAL_EDGE", "SEF", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("154040_MDC_EEG_PWR_SPEC_TOT", "TP", DoubleType.GLOBAL));
        metadata.addProperty(new JetLinksPropertyMetadata("154028_MDC_EEG_NUM_SPK", "BC", IntType.GLOBAL));

        // Waveforms (Table 37)
        metadata.addProperty(new JetLinksPropertyMetadata("131329_MDC_ECG_ELEC_POTL_I", "ECG Lead I", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131330_MDC_ECG_ELEC_POTL_II", "ECG Lead II", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131389_MDC_ECG_ELEC_POTL_III", "ECG Lead III", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131390_MDC_ECG_ELEC_POTL_AVR", "ECG Lead aVR", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131391_MDC_ECG_ELEC_POTL_AVL", "ECG Lead aVL", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131392_MDC_ECG_ELEC_POTL_AVF", "ECG Lead aVF", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131331_MDC_ECG_ELEC_POTL_V1", "ECG Lead V1", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131332_MDC_ECG_ELEC_POTL_V2", "ECG Lead V2", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131333_MDC_ECG_ELEC_POTL_V3", "ECG Lead V3", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131334_MDC_ECG_ELEC_POTL_V4", "ECG Lead V4", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131335_MDC_ECG_ELEC_POTL_V5", "ECG Lead V5", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131336_MDC_ECG_ELEC_POTL_V6", "ECG Lead V6", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("131395_MDC_ECG_ELEC_POTL_V", "ECG Lead V", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("377_MNDRY_ECG_ELEC_POTL_VB", "ECG Lead VB", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150452_MDC_PULS_OXIM_PLETH", "Pleth 1", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("151780_MDC_IMPED_TTHOR", "Transthoracic Impedance", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150032_MDC_PRESS_BLD_ART", "Arterial Blood Pressure1", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150044_MDC_PRESS_BLD_ART_PULM", "Pulmonary Arterial Blood Pressure", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150084_MDC_PRESS_BLD_VEN_CENT", "Central Venous Blood Pressure", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150068_MDC_PRESS_BLD_ATR_RIGHT", "Right Atria Blood Pressure", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150064_MDC_PRESS_BLD_ATR_LEFT", "Left Atria Blood Pressure", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("153608_MDC_PRESS_INTRA_CRAN", "Intra Cranial Pressure", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("150016_MDC_PRESS_BLD", "Invasive Blood Pressure1", new ArrayType()));
        metadata.addProperty(new JetLinksPropertyMetadata("151700_MDC_CONC_AWAY_CO2", "CO2 CO2, Airway", new ArrayType()));

        return metadata;
    }

    @Override
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("hl7-2.6-protocol");
        support.setName("hl7-2.6-protocol");

        // 配置编解码
        support.addMessageCodecSupport(new TcpDeviceMessageCodec(context));

        //开启diffMetadataSameProduct特性
        support.addFeature(MetadataFeature.diffMetadataSameProduct);

        // 配置设备元数据
        support.addDefaultMetadata(DefaultTransport.TCP, createDeviceMetadata());

        //设置配置定义信息
        support.addConfigMetadata(DefaultTransport.TCP, new DefaultConfigMetadata(
            "TCP"
            , ""));

        return Mono.just(support);
    }
}