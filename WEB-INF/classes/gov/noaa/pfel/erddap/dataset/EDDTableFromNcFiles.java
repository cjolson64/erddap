/* 
 * EDDTableFromNcFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents a table of data from a collection of n-dimensional (1,2,3,4,...) .nc data files.
 * The dimensions are e.g., time,depth,lat,lon.
 * <br>A given file may have multiple values for each of the dimensions 
 *   and the values may be different in different files.
 * <br>[Was: only the leftmost dimension (e.g., time) could have multiple values.]
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-13
 */
public class EDDTableFromNcFiles extends EDDTableFromFiles { 


    /** 
     * The constructor just calls the super constructor. 
     *
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     */
    public EDDTableFromNcFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ) 
        throws Throwable {

        super("EDDTableFromNcFiles", true, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File,
            tAddGlobalAttributes, tAltMetersPerSourceUnit, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ);

    }

    /** 
     * The constructor. 
     *
     */
    public EDDTableFromNcFiles(String tClassName, boolean tFilesAreLocal,
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ) 
        throws Throwable {

        super(tClassName, tFilesAreLocal, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File,
            tAddGlobalAttributes, tAltMetersPerSourceUnit, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ);

    }


    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     *
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        //Future: more efficient if !mustGetData is handled differently

        //read the file
        Table table = new Table();
        table.readNDNc(fileDir + fileName, sourceDataNames.toArray(),
            sortedSpacing >= 0 && !Double.isNaN(minSorted)? sortedColumnSourceName : null,
                minSorted, maxSorted, 
            getMetadata);
        //String2.log("  EDDTableFromNcFiles.getSourceDataFromFile table.nRows=" + table.nRows());

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromNcFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (usually ".*\\.nc").
     * @param sampleFileName the full file name of one of the files in the collection
     * @param useDimensionsCSV If null or "", this finds the group of variables sharing the
     *    highest number of dimensions. Otherwise, it find the variables using
     *    these dimensions (plus related char variables).
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @throws Throwable if trouble
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String useDimensionsCSV,
        int tReloadEveryNMinutes,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromNcFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        String[] useDimensions = StringArray.arrayFromCSV(useDimensionsCSV);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //new way
        StringArray varNames = new StringArray();
        if (useDimensions.length > 0) {
            //find the varNames
            NetcdfFile ncFile = NcHelper.openFile(sampleFileName);
            try {

                Group rootGroup = ncFile.getRootGroup();
                List rootGroupVariables = rootGroup.getVariables(); 
                for (int v = 0; v < rootGroupVariables.size(); v++) {
                    Variable var = (Variable)rootGroupVariables.get(v);
                    boolean isChar = var.getDataType() == DataType.CHAR;
                    if (var.getRank() + (isChar? -1 : 0) == useDimensions.length) {
                        boolean matches = true;
                        for (int d = 0; d < useDimensions.length; d++) {
                            if (!var.getDimension(d).getName().equals(useDimensions[d])) {
                                matches = false;
                                break;
                            }
                        }
                        if (matches) 
                            varNames.add(var.getShortName());
                    }
                }
                ncFile.close(); 

            } catch (Exception e) {
                //make sure ncFile is explicitly closed
                try {
                    ncFile.close(); 
                } catch (Exception e2) {
                    //don't care
                }
                String2.log(MustBe.throwableToString(e)); 
            }
            Test.ensureTrue(varNames.size() > 0, 
                "The file has no variables with dimensions: " + useDimensionsCSV);
        }

        //then read the file
        dataSourceTable.readNDNc(sampleFileName, varNames.toStringArray(), 
            null, 0, 0, true); //getMetadata
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                dataSourceTable.getColumn(c),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    sourceAtts, colName, true, true)); //addColorBarMinMax, tryToFindLLAT

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                tSortedColumnSourceName = colName;
        }
        //String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
        //String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the axisAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + suggestedRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + tPreExtractRegex + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + tPostExtractRegex + "</postExtractRegex>\n" +
            "    <extractRegex>" + tExtractRegex + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }


    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                1440,
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", "", null);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromNcFiles",
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                "1440",
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", ""},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromNcFiles\" datasetID=\"ndbcMet_5df7_b363_ad99\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <fileDir>C:/u00/data/points/ndbcMet/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex>^.{5}</preExtractRegex>\n" +
"    <postExtractRegex>.{7}$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID TIME</sortFilesBySourceNames>\n" +
"    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"cdm_data_type\">Station</att>\n" +
"        <att name=\"contributor_name\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"contributor_role\">Source of data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"date_created\">2012-03-22Z</att>\n" + //changes
"        <att name=\"date_issued\">2012-03-22Z</att>\n" +  //changes
"        <att name=\"Easternmost_Easting\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">down</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NOAA NDBC</att>\n" +
"        <att name=\"id\">NDBC_41004_met</att>\n" +
"        <att name=\"institution\">NOAA National Data Buoy Center and Participators in Data Assembly Center.</att>\n" +
"        <att name=\"keywords\">Oceans</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"NDBCMeasurementDescriptionUrl\">http://www.ndbc.noaa.gov/measdes.shtml</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"project\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"quality\">Automated QC checks with periodic manual QC</att>\n" +
"        <att name=\"source\">station observation</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">The National Data Buoy Center (NDBC) distributes meteorological data from moored buoys maintained by NDBC and others. Moored buoys are the weather sentinels of the sea. They are deployed in the coastal and offshore waters from the western Atlantic to the Pacific Ocean around Hawaii, and from the Bering Sea to the South Pacific. NDBC&#039;s moored buoys measure and transmit barometric pressure; wind direction, speed, and gust; air and sea temperature; and wave energy spectra from which significant wave height, dominant wave period, and average wave period are derived. Even the direction of wave propagation is measured on many moored buoys. \n" +
"\n" +                                                            //changes 2 places...
"This dataset has both historical data (quality controlled, before 2012-03-01T00:00:00) and near real time data (less quality controlled, from 2012-03-01T00:00:00 on).</att>\n" +
"        <att name=\"time_coverage_end\">2012-03-22T16:00:00Z</att>\n" + //changes
"        <att name=\"time_coverage_resolution\">P1H</att>\n" +
"        <att name=\"time_coverage_start\">1978-06-27T13:00:00Z</att>\n" +
"        <att name=\"title\">NOAA NDBC Standard Meteorological</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"float\">-79.099</att>\n" +
"    </sourceAttributes -->\n" +
cdmSuggestion() +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"keywords\">\n" +
"Atmosphere &gt; Air Quality &gt; Visibility,\n" +
"Atmosphere &gt; Altitude &gt; Planetary Boundary Layer Height,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Pressure Tendency,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" +
"Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" +
"Oceans &gt; Ocean Waves &gt; Swells,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Period,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Speed/Direction,\n" +
"air, air_pressure_at_sea_level, air_temperature, altitude, assembly, atmosphere, atmospheric, average, boundary, buoy, center, center., currents, data, depth, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, national, ndbc, noaa, northward, northward_wind, ocean, oceans, participators, period, planetary, pressure, quality, sea, sea_surface_swell_wave_period, sea_surface_temperature, sea_surface_wave_significant_height, sea_surface_wave_to_direction, seawater, significant, speed, sst, standard, static, station, station_id, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>stationID</sourceName>\n" +
"        <destinationName>stationID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIME</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">2.678004E8 1.332432E9</att>\n" + //changes
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"comment\">Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.5E9</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEPTH</sourceName>\n" +
"        <destinationName>DEPTH</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">down</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"comment\">The depth of the station, nominally 0 (see station information for details).</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LAT</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">32.501 32.501</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"comment\">The latitude of the station.</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LON</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-79.099 -79.099</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"comment\">The longitude of the station.</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WD</sourceName>\n" +
"        <destinationName>WD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPD</sourceName>\n" +
"        <destinationName>WSPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 26.0</att>\n" +
"            <att name=\"comment\">Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>GST</sourceName>\n" +
"        <destinationName>GST</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 33.9</att>\n" +
"            <att name=\"comment\">Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload, See the Sensor Reporting, Sampling, and Accuracy section.</att>\n" +
"            <att name=\"long_name\">Wind Gust Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed_of_gust</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WVHT</sourceName>\n" +
"        <destinationName>WVHT</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 12.53</att>\n" +
"            <att name=\"comment\">Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Height</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_wave_significant_height</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DPD</sourceName>\n" +
"        <destinationName>DPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 20.0</att>\n" +
"            <att name=\"comment\">Dominant wave period (seconds) is the period with the maximum wave energy. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Dominant</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>APD</sourceName>\n" +
"        <destinationName>APD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 13.1</att>\n" +
"            <att name=\"comment\">Average wave period (seconds) of all waves during the 20-minute period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Average</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MWD</sourceName>\n" +
"        <destinationName>MWD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Mean wave direction corresponding to energy of the dominant period (DOMPD). The units are degrees from true North just like wind direction. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">sea_surface_wave_to_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>BAR</sourceName>\n" +
"        <destinationName>BAR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">976.5 1041.5</att>\n" +
"            <att name=\"comment\">Air pressure (hPa). (&#039;PRES&#039; on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n" +
"            <att name=\"long_name\">Air Pressure</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1030.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">970.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ATMP</sourceName>\n" +
"        <destinationName>ATMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 31.7</att>\n" +
"            <att name=\"comment\">Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.</att>\n" +
"            <att name=\"long_name\">Air Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WTMP</sourceName>\n" +
"        <destinationName>WTMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 32.2</att>\n" +
"            <att name=\"comment\">Sea surface temperature (Celsius). For sensor depth, see Hull Description.</att>\n" +
"            <att name=\"long_name\">SST</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEWP</sourceName>\n" +
"        <destinationName>DEWP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-10.6 29.1</att>\n" +
"            <att name=\"comment\">Dewpoint temperature taken at the same height as the air temperature measurement.</att>\n" +
"            <att name=\"long_name\">Dewpoint Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">dew_point_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>VIS</sourceName>\n" +
"        <destinationName>VIS</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 58.1</att>\n" +
"            <att name=\"comment\">Station visibility (km, originally statute miles). Note that buoy stations are limited to reports from 0 to 1.9 miles.</att>\n" +
"            <att name=\"long_name\">Station Visibility</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">visibility_in_air</att>\n" +
"            <att name=\"units\">km</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PTDY</sourceName>\n" +
"        <destinationName>PTDY</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-3.7 5.0</att>\n" +
"            <att name=\"comment\">Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.</att>\n" +
"            <att name=\"long_name\">Pressure Tendency</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">tendency_of_air_pressure</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-3.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIDE</sourceName>\n" +
"        <destinationName>TIDE</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"comment\">The water level in meters (originally feet) above or below Mean Lower Low Water (MLLW).</att>\n" +
"            <att name=\"long_name\">Water Level</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">surface_altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPU</sourceName>\n" +
"        <destinationName>WSPU</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-17.9 21.0</att>\n" +
"            <att name=\"comment\">The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Zonal</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPV</sourceName>\n" +
"        <destinationName>WSPV</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-25.0 20.9</att>\n" +
"            <att name=\"comment\">The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Meridional</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ID</sourceName>\n" +
"        <destinationName>ID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">The station identifier.</att>\n" +
"            <att name=\"long_name\">Station Identifier</att>\n" +
"            <att name=\"standard_name\">station_id</att>\n" +
"            <att name=\"units\">unitless</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //with one small change to addAttributes:
            results = String2.replaceAll(results, 
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n",
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
                "        <att name=\"cdm_data_type\">Other</att>\n");
            String2.log(results);

            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "ndbcMet_5df7_b363_ad99", "");
            Test.ensureEqual(edd.title(), "NOAA NDBC Standard Meteorological", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, time, DEPTH, latitude, longitude, WD, WSPD, GST, WVHT, " +
                "DPD, APD, MWD, BAR, ATMP, WTMP, DEWP, VIS, PTDY, TIDE, WSPU, WSPV, ID", 
                "");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * testGenerateDatasetsXml2
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
        "f:/data/ngdcJasonSwath/", ".*\\.nc", 
        "f:/data/ngdcJasonSwath/JA2_OPN_2PcS088_239_20101201_005323_20101201_025123.nc", 
        "time",  //not "time, meas_ind"
        10080, 
        "", "", "", 
        "", "", 
        "time", 
        "", "", "", "", new Attributes());

            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "ngdcJasonSwath_2743_941d_6d6c", "");
            Test.ensureEqual(edd.title(), "OGDR - Standard dataset", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"time, latitude, longitude, surface_type, alt_echo_type, rad_surf_type, " +
"qual_alt_1hz_range_ku, qual_alt_1hz_range_c, qual_alt_1hz_swh_ku, qual_alt_1hz_swh_c, " +
"qual_alt_1hz_sig0_ku, qual_alt_1hz_sig0_c, qual_alt_1hz_off_nadir_angle_wf_ku, " +
"qual_alt_1hz_off_nadir_angle_pf, qual_inst_corr_1hz_range_ku, qual_inst_corr_1hz_range_c, " +
"qual_inst_corr_1hz_swh_ku, qual_inst_corr_1hz_swh_c, qual_inst_corr_1hz_sig0_ku, " +
"qual_inst_corr_1hz_sig0_c, qual_rad_1hz_tb187, qual_rad_1hz_tb238, qual_rad_1hz_tb340, " +
"alt_state_flag_oper, alt_state_flag_c_band, alt_state_flag_band_seq, " +
"alt_state_flag_ku_band_status, alt_state_flag_c_band_status, rad_state_flag_oper, " +
"orb_state_flag_diode, orb_state_flag_rest, ecmwf_meteo_map_avail, rain_flag, ice_flag, " +
"interp_flag_tb, interp_flag_mean_sea_surface, interp_flag_mdt, " +
"interp_flag_ocean_tide_sol1, interp_flag_ocean_tide_sol2, interp_flag_meteo, alt, " +
"orb_alt_rate, range_ku, range_c, range_rms_ku, range_rms_c, range_numval_ku, " +
"range_numval_c, net_instr_corr_range_ku, net_instr_corr_range_c, model_dry_tropo_corr, " +
"model_wet_tropo_corr, rad_wet_tropo_corr, iono_corr_alt_ku, iono_corr_gim_ku, " +
"sea_state_bias_ku, sea_state_bias_c, swh_ku, swh_c, swh_rms_ku, swh_rms_c, " +
"swh_numval_ku, swh_numval_c, net_instr_corr_swh_ku, net_instr_corr_swh_c, sig0_ku, " +
"sig0_c, sig0_rms_ku, sig0_rms_c, sig0_numval_ku, sig0_numval_c, agc_ku, agc_c, " +
"agc_rms_ku, agc_rms_c, agc_numval_ku, agc_numval_c, net_instr_corr_sig0_ku, " +
"net_instr_corr_sig0_c, atmos_corr_sig0_ku, atmos_corr_sig0_c, off_nadir_angle_wf_ku, " +
"off_nadir_angle_pf, tb_187, tb_238, tb_340, mean_sea_surface, mean_topography, geoid, " +
"bathymetry, inv_bar_corr, hf_fluctuations_corr, ocean_tide_sol1, ocean_tide_sol2, " +
"ocean_tide_equil, ocean_tide_non_equil, load_tide_sol1, load_tide_sol2, " +
"solid_earth_tide, pole_tide, wind_speed_model_u, wind_speed_model_v, " +
"wind_speed_alt, wind_speed_rad, rad_water_vapor, rad_liquid_water, ssha", 
                ""); 

        } catch (Throwable t) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * This tests the methods in this class with a 1D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test1D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test1D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String id = "erdCinpKfmSFNH";
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetDir(id) + BADFILE_TABLE_FILENAME);
        }
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles 1D test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station Identifier\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -120.4, -118.4;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum -118.4;\n" +
"    Float64 colorBarMinimum -120.4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 32.8, 34.05;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 34.5;\n" +
"    Float64 colorBarMinimum 32.5;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range -17.0, -5.0;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 0.0;\n" +
"    Float64 colorBarMinimum -20.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 4.89024e+8, 1.183248e+9;\n" +
"    String axis \"T\";\n" +
"    Float64 colorBarMaximum 1.183248e+9;\n" +
"    Float64 colorBarMinimum 4.89024e+8;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  common_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Common Name\";\n" +
"  }\n" +
"  species_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Species Name\";\n" +
"  }\n" +
"  size {\n" +
"    Int16 actual_range 1, 385;\n" +
"    String ioos_category \"Biology\";\n" +
"    String long_name \"Size\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD, Channel Islands National Park, National Park Service\";\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"id, longitude, latitude\";\n" +
"    String contributor_email \"David_Kushner@nps.gov\";\n" +
"    String contributor_name \"Channel Islands National Park, National Park Service\";\n" +
"    String contributor_role \"Source of data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String creator_email \"Roy.Mendelssohn@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_url \"http://www.pfel.noaa.gov\";\n" +
"    String date_created \"2008-06-11T21:43:28Z\";\n" +
"    String date_issued \"2008-06-11T21:43:28Z\";\n" +
"    Float64 Easternmost_Easting -118.4;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 34.05;\n" +
"    Float64 geospatial_lat_min 32.8;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -118.4;\n" +
"    Float64 geospatial_lon_min -120.4;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max -5.0;\n" +
"    Float64 geospatial_vertical_min -17.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Channel Islands National Park, National Park Service\n" +
"2008-06-11T21:43:28Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" + //will be SWFSC when reprocessed
today + " (local files)\n" +
today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/erdCinpKfmSFNH.das\";\n" +
"    String infoUrl \"http://www.nps.gov/chis/naturescience/index.htm\";\n" +
"    String institution \"CINP\";\n" +
"    String keywords \"Atmosphere > Altitude > Station Height,\n" +
"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"altitude, aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.  National Park Service Disclaimer: The National Park Service shall not be held liable for improper or incorrect use of the data described and/or contained herein. These data and related graphics are not legal documents and are not intended to be used as such. The information contained in these data is dynamic and may change over time. The data are not better than the original sources from which they were derived. It is the responsibility of the data user to use the data appropriately and consistent within the limitation of geospatial data in general and these data in particular. The related graphics are intended to aid the data user in acquiring relevant data; it is not appropriate to use the related graphics as data. The National Park Service gives no warranty, expressed or implied, as to the accuracy, reliability, or completeness of these data. It is strongly recommended that these data are directly acquired from an NPS server and not indirectly through other sources which may have changed the data in some way. Although these data have been processed successfully on computer systems at the National Park Service, no warranty expressed or implied is made regarding the utility of the data on other systems for general or scientific purposes, nor shall the act of distribution constitute any such warranty. This disclaimer applies both to individual use of the data and aggregate use with other data.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 34.05;\n" +
"    String observationDimension \"row\";\n" +
"    String project \"NOAA NMFS SWFSC ERD (http://www.pfel.noaa.gov/)\";\n" +
"    String references \"Channel Islands National Parks Inventory and Monitoring information: http://nature.nps.gov/im/units/medn . Kelp Forest Monitoring Protocols: http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 32.8;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" + 
"    String subsetVariables \"id, longitude, latitude, common_name, species_name\";\n" +
"    String summary \"This dataset has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. Sampling is conducted annually between the months of May-October, so the Time data in this file is July 1 of each year (a nominal value). The size frequency measurements were taken within 10 meters of the transect line at each site.  Depths at the site vary some, but we describe the depth of the site along the transect line where that station's temperature logger is located, a typical depth for the site.\";\n" +
"    String time_coverage_end \"2007-07-01T00:00:00Z\";\n" +
"    String time_coverage_start \"1985-07-01T00:00:00Z\";\n" +
"    String title \"Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat\";\n" +
"    Float64 Westernmost_Easting -120.4;\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String id;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 altitude;\n" +
"    Float64 time;\n" +
"    String common_name;\n" +
"    String species_name;\n" +
"    Int16 size;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test 1D make DATA FILES\n");       

        //.csv    for one lat,lon,time
        userDapQuery = "" +
            "&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1Station", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,altitude,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv    for one lat,lon,time      via lon > <
        userDapQuery = "" +
            "&longitude>-119.06&longitude<=-119.04&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,altitude,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,-14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species
        userDapQuery = "" +
            "&time=2005-07-01&common_name=\"Red+abalone\"";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_eq", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,altitude,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,-5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,-13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,-9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,-9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species    String !=
        userDapQuery = "" +
            "&time=2005-07-01&id!=\"San+Miguel+(Hare+Rock)\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_NE", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,altitude,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,-10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,-13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,-9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,-9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species   String > <
        userDapQuery = "" +
            "&time=2005-07-01&id>\"San+Miguel+(G\"&id<=\"San+Miguel+(I\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_gtlt", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,altitude,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,-5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species     REGEX
        userDapQuery = "longitude,latitude,altitude,time,id,species_name,size" + //no common_name
            "&time=2005-07-01&id=~\"(zztop|.*Hare+Rock.*)\"&common_name=\"Red+abalone\"";   //but common_name here
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_regex", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,altitude,time,id,species_name,size\n" +
"degrees_east,degrees_north,m,UTC,,,mm\n" +
"-120.35,34.05,-5.0,2005-07-01T00:00:00Z,San Miguel (Hare Rock),Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests the methods in this class with a 2D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test2D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test2D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //the test files were made with makeTestFiles();
        String id = "testNc2D";       
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetDir(id) + BADFILE_TABLE_FILENAME);
        }

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/nc2d/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/nc2d/NDBC_4D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 
        //just comment out when working on datasets below
/* currently not active
        Test.ensureTrue(eddTable.sosOfferings().indexOf("41002") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles 2D test das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 8.67456e+7, 1.2075984e+9;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wd {[10]\n" +
"    Int16 _FillValue 32767;[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wspv {[10]\n" +
"    Float32 _FillValue -9999999.0;[10]\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;[10]  
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"    String comment \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed, Meridional\";[10]\n" +
"    Float32 missing_value -9999999.0;[10]\n" +
"    String standard_name \"northward_wind\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +      //no altitude or longitude
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test2D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude=-27.7&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-27.7,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat area
        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"35.01,2005-04-01T00:00:00Z,41025,1.34,10.0,9.1,14.9\n" +
"38.47,2005-04-01T00:00:00Z,44004,2.04,11.43,9.8,4.9\n" +
"38.46,2005-04-01T00:00:00Z,44009,1.3,10.0,5.0,5.7\n" +
"36.61,2005-04-01T00:00:00Z,44014,1.67,11.11,6.5,8.6\n" +
"37.36,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"38.23,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"37.75,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"35.74,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"36.75,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"37.98,2005-04-01T00:00:00Z,46059,2.51,14.29,12.9,NaN\n" +
"36.83,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"37.57,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n" +
"35.21,2005-04-01T00:00:00Z,46215,1.4,10.0,11.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,,m,s,degree_C,degree_C\n" +
"35.01,41025,1.34,10.0,9.1,14.9\n" +
"38.47,44004,2.04,11.43,9.8,4.9\n" +
"38.46,44009,1.3,10.0,5.0,5.7\n" +
"36.61,44014,1.67,11.11,6.5,8.6\n" +
"37.36,46012,2.55,12.5,13.7,NaN\n" +
"38.23,46013,2.3,12.9,13.9,NaN\n" +
"37.75,46026,1.96,12.12,14.0,NaN\n" +
"35.74,46028,2.57,12.9,16.3,NaN\n" +
"36.75,46042,2.21,17.39,14.5,NaN\n" +
"37.98,46059,2.51,14.29,12.9,NaN\n" +
"36.83,46091,NaN,NaN,NaN,NaN\n" +
"36.75,46092,NaN,NaN,NaN,NaN\n" +
"36.69,46093,NaN,NaN,14.3,NaN\n" +
"37.57,46214,2.5,9.0,12.8,NaN\n" +
"35.21,46215,1.4,10.0,11.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,wtmp\n" +
"degrees_north,degree_C\n" +
"32.31,23.5\n" +
"28.5,21.3\n" +
"28.95,23.7\n" +
"30.0,20.1\n" +
"14.53,25.3\n" +
"20.99,25.7\n" +
"27.47,23.8\n" +
"31.9784,22.0\n" +
"28.4,21.0\n" +
"27.55,21.8\n" +
"25.9,24.1\n" +
"25.17,23.9\n" +
"26.07,26.1\n" +
"22.01,24.4\n" +
"19.87,26.8\n" +
"15.01,26.4\n" +
"27.3403,20.2\n" +
"29.06,21.8\n" +
"38.47,20.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * This tests the methods in this class with a 3D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test3D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test3D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String id = "testNc3D";
        
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetDir(id) + BADFILE_TABLE_FILENAME);
        }

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/nc3d/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/nc3d/NDBC_4D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 
        //just comment out when working on datasets below
/* currently not active
        //test sos-server values
        Test.ensureTrue(eddTable.sosOfferings().indexOf("32012") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles test3D das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 8.67456e+7, 1.2075984e+9;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wd {[10]\n" +
"    Int16 _FillValue 32767;[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wspv {[10]\n" +
"    Float32 _FillValue -9999999.0;[10]\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;[10]  
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"    String comment \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed, Meridional\";[10]\n" +
"    Float32 missing_value -9999999.0;[10]\n" +
"    String standard_name \"northward_wind\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +      //no altitude
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test3D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.13&latitude=-27.7&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-48.13,-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.13&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.13,-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.13,-27.7,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat lon area
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"-123.32,38.23,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "longitude,latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,46012,2.55,12.5,13.7,NaN\n" +
"-123.32,38.23,46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,46214,2.5,9.0,12.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,wtmp\n" +
"degrees_east,degrees_north,degree_C\n" +
"-75.35,32.31,23.5\n" +
"-80.17,28.5,21.3\n" +
"-78.47,28.95,23.7\n" +
"-80.6,30.0,20.1\n" +
"-46.0,14.53,25.3\n" +
"-65.01,20.99,25.7\n" +
"-71.49,27.47,23.8\n" +
"-69.649,31.9784,22.0\n" +
"-80.53,28.4,21.0\n" +
"-80.22,27.55,21.8\n" +
"-89.67,25.9,24.1\n" +
"-94.42,25.17,23.9\n" +
"-85.94,26.07,26.1\n" +
"-94.05,22.01,24.4\n" +
"-85.06,19.87,26.8\n" +
"-67.5,15.01,26.4\n" +
"-84.245,27.3403,20.2\n" +
"-88.09,29.06,21.8\n" +
"-70.56,38.47,20.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * One time: make c:/u00/data/points/nc2d and String2.testU00Dir/u00/data/points/nc3d test files 
     * from c:/u00/data/points/ndbcMet nc4d files.
     */
    public static void makeTestFiles() {
        //get list of files
        String fromDir = "c:/u00/data/points/ndbcMet/";
        String dir3 =    "c:/u00/data/points/nc3d/";
        String dir2 =    "c:/u00/data/points/nc2d/";
        String[] names = RegexFilenameFilter.list(fromDir, "NDBC_(3|4).*nc");        
        
        //for each file
        for (int f = 0; f < names.length; f++) {
            NetcdfFile in = null;
            NetcdfFileWriteable out2 = null;
            NetcdfFileWriteable out3 = null;

            try {
                String2.log("in #" + f + "=" + fromDir + names[f]);
                if (f == 0) 
                    String2.log(NcHelper.dumpString(fromDir + names[f], false));

                in = NcHelper.openFile(fromDir + names[f]);
                out2 = NetcdfFileWriteable.createNew(dir2 + names[f], false);
                out3 = NetcdfFileWriteable.createNew(dir3 + names[f], false);

                //write the globalAttributes
                Attributes atts = new Attributes();
                NcHelper.getGlobalAttributes(in, atts);
                NcHelper.setAttributes(out2, "NC_GLOBAL", atts);
                NcHelper.setAttributes(out3, "NC_GLOBAL", atts);

                Variable vars[] = NcHelper.find4DVariables(in, null);
                Variable timeVar = in.findVariable("TIME");
                Variable latVar  = in.findVariable("LAT");
                Variable lonVar  = in.findVariable("LON");
                EDStatic.ensureArraySizeOkay(timeVar.getSize(), "EDDTableFromNcFiles.makeTestFiles");
                Dimension tDim2 = out2.addDimension("TIME", (int)timeVar.getSize()); //safe since checked above
                Dimension tDim3 = out3.addDimension("TIME", (int)timeVar.getSize()); //safe since checked above
                Dimension yDim2 = out2.addDimension("LAT", 1);
                Dimension yDim3 = out3.addDimension("LAT", 1);
                Dimension xDim3 = out3.addDimension("LON", 1);
                
                //create axis variables
                out2.addVariable("TIME", timeVar.getDataType(), new Dimension[]{tDim2}); 
                out2.addVariable("LAT",  latVar.getDataType(),  new Dimension[]{yDim2}); 

                out3.addVariable("TIME", timeVar.getDataType(), new Dimension[]{tDim3}); 
                out3.addVariable("LAT",  latVar.getDataType(),  new Dimension[]{yDim3}); 
                out3.addVariable("LON",  lonVar.getDataType(),  new Dimension[]{xDim3}); 

                //write the axis variable attributes
                atts.clear();
                NcHelper.getVariableAttributes(timeVar, atts);
                NcHelper.setAttributes(out2, "TIME", atts);
                NcHelper.setAttributes(out3, "TIME", atts);

                atts.clear();
                NcHelper.getVariableAttributes(latVar, atts);
                NcHelper.setAttributes(out2, "LAT", atts);
                NcHelper.setAttributes(out3, "LAT", atts);

                atts.clear();
                NcHelper.getVariableAttributes(lonVar, atts);
                NcHelper.setAttributes(out2, "LON", atts);
                NcHelper.setAttributes(out3, "LON", atts);

                //create data variables
                for (int col = 0; col < vars.length; col++) {
                    //create the data variables
                    Variable var = vars[col];
                    String varName = var.getName();
                    Array ar = var.read();
                    out2.addVariable(varName, var.getDataType(), new Dimension[]{tDim2, yDim2}); 
                    out3.addVariable(varName, var.getDataType(), new Dimension[]{tDim3, yDim3, xDim3}); 

                    //write the data variable attributes
                    atts.clear();
                    NcHelper.getVariableAttributes(var, atts);
                    NcHelper.setAttributes(out2, varName, atts);
                    NcHelper.setAttributes(out3, varName, atts);
                }

                //leave "define" mode
                out2.create();
                out3.create();

                //write axis data
                Array ar = in.findVariable("TIME").read();
                out2.write("TIME", ar);
                out3.write("TIME", ar);
                ar = in.findVariable("LAT").read();
                out2.write("LAT", ar);
                out3.write("LAT", ar);
                ar = in.findVariable("LON").read();
                out3.write("LON", ar);
                 
                for (int col = 0; col < vars.length; col++) {
                    //write the data for each var
                    Variable var = vars[col];
                    String varName = var.getName();
                    ar = var.read();
                    int oldShape[] = ar.getShape();
                    int newShape2[] = {oldShape[0], 1};
                    int newShape3[] = {oldShape[0], 1, 1};
                    out2.write(varName, ar.reshape(newShape2));
                    out3.write(varName, ar.reshape(newShape3));
                }

                in.close();
                out2.close();
                out3.close();

                if (f == 0) {
                    String2.log("\nout2=" + NcHelper.dumpString(dir2 + names[f], false));
                    String2.log("\nout3=" + NcHelper.dumpString(dir3 + names[f], false));
                }

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
                try { if (in != null)  in.close();    } catch (Exception t2) {}
                try { if (out2 != null) out2.close(); } catch (Exception t2) {}
                try { if (out3 != null) out3.close(); } catch (Exception t2) {}
            }
        }
    }

    /**
     * This tests the methods in this class with a 4D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test4D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test4D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String id = "cwwcNDBCMet";
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetDir(id) + BADFILE_TABLE_FILENAME);
        }
        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/ndbcMet/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/ndbcMet/NDBC_3D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 
        //just comment out when working on datasets below
/* currently not active
        Test.ensureTrue(eddTable.sosOfferings().indexOf("32012") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles test4D das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);

        expected = 
"  wd {\n" +
"    Int16 _FillValue 32767;\n";
        po = results.indexOf(expected.substring(0,10));
        if (po < 0) String2.log("\nresults=\n" + results);
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);

        expected = 
"  wvht {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 92.39;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during " +
"the 20-minute sampling period. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Height\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0,10));
        if (po < 0) String2.log("\nresults=\n" + results);
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);

        expected = 
"  wspv {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;  
        po = results.indexOf(expected.substring(0,10));
        if (po < 0) String2.log("\nresults=\n" + results);
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);

        expected = 
"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed, Meridional\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"northward_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n";
        po = results.indexOf(expected.substring(0,10));
        if (po < 0) String2.log("\nresults=\n" + results);
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test4D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        
        //2011-04-12 was -48.13&latitude=-27.7  now 27.705 S 48.134 W
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.134&latitude=-27.705&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-48.134,-27.705,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.134&latitude=-27.705&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.134,-27.705,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.134,-27.705,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat lon area
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-122.881,37.363,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"-123.301,38.242,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"-122.833,37.759,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"-121.884,35.741,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"-122.404,36.789,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"-121.899,36.835,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"-123.47,37.945,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n" +
"-122.298,37.772,2005-04-01T00:00:00Z,AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.708,38.913,2005-04-01T00:00:00Z,ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.465,37.807,2005-04-01T00:00:00Z,FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.888,36.605,2005-04-01T00:00:00Z,MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.038,38.057,2005-04-01T00:00:00Z,PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.955,2005-04-01T00:00:00Z,PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.928,2005-04-01T00:00:00Z,RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.507,2005-04-01T00:00:00Z,RTYC1,NaN,NaN,14.2,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "longitude,latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,,m,s,degree_C,degree_C\n" +
"-122.881,37.363,46012,2.55,12.5,13.7,NaN\n" +
"-123.301,38.242,46013,2.3,12.9,13.9,NaN\n" +
"-122.833,37.759,46026,1.96,12.12,14.0,NaN\n" +
"-121.884,35.741,46028,2.57,12.9,16.3,NaN\n" +
"-122.404,36.789,46042,2.21,17.39,14.5,NaN\n" +
"-121.899,36.835,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,46093,NaN,NaN,14.3,NaN\n" +
"-123.47,37.945,46214,2.5,9.0,12.8,NaN\n" +
"-122.298,37.772,AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.708,38.913,ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.465,37.807,FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.888,36.605,MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.038,38.057,PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.955,PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.928,RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.507,RTYC1,NaN,NaN,14.2,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,wtmp\n" +
"degrees_east,degrees_north,degree_C\n" +
"-80.166,28.519,21.2\n" +
"-78.471,28.906,23.7\n" +
"-80.533,30.041,20.1\n" +
"-46.008,14.357,25.3\n" +
"-64.966,21.061,25.7\n" +
"-71.491,27.469,23.8\n" +
"-69.649,31.978,22.0\n" +
"-80.53,28.4,21.6\n" +
"-80.225,27.551,21.7\n" +
"-89.658,25.888,24.1\n" +
"-93.666,25.79,23.9\n" +
"-85.612,26.044,26.1\n" +
"-94.046,22.017,24.4\n" +
"-85.059,19.874,26.8\n" +
"-67.472,15.054,26.4\n" +
"-84.245,27.34,20.2\n" +
"-88.09,29.06,21.7\n" +
"-157.808,17.094,24.3\n" +
"-160.66,19.087,24.7\n" +
"-152.382,17.525,24.0\n" +
"-153.913,0.0,25.0\n" +
"-158.116,21.673,24.3\n" +
"-157.668,21.417,24.2\n" +
"144.789,13.354,28.1\n" +
"-90.42,29.789,20.4\n" +
"-64.92,18.335,27.7\n" +
"-81.872,26.647,22.2\n" +
"-80.097,25.59,23.5\n" +
"-156.472,20.898,25.0\n" +
"167.737,8.737,27.6\n" +
"-81.808,24.553,23.9\n" +
"-80.862,24.843,23.8\n" +
"-64.753,17.697,26.0\n" +
"-67.047,17.972,27.1\n" +
"-80.38,25.01,24.2\n" +
"-81.807,26.13,23.7\n" +
"-170.688,-14.28,29.6\n" +
"-157.867,21.307,25.5\n" +
"-96.388,28.452,20.1\n" +
"-82.773,24.693,22.8\n" +
"-97.215,26.06,20.1\n" +
"-82.627,27.76,21.7\n" +
"-66.117,18.462,28.3\n" +
"-177.36,28.212,21.8\n" +
"-80.593,28.415,22.7\n" +
"166.618,19.29,27.9\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This is run by hand (special setup) to test dealing with the last 24 hours.
     * Before running this, run NDBCMet updateLastNDays, then copy some files to /u00/data/points/ndbcMet
     * so files have very recent data.
     *
     * @throws Throwable if trouble
     */
    public static void test24Hours() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test24Hours() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //!!!change time to be ~nowLocal+16 (= nowZulu+8);  e.g., T20 for local time 4pm
        userDapQuery = "longitude,latitude,time,station,wd,wtmp&time%3E=2009-03-12T20"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_24hours", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        //in log output, look at end of constructor for "maxTime is within last 24hrs, so setting maxTime to NaN (i.e., Now)."
        //in log output, look for stations saying "file maxTime is within last 24hrs, so ERDDAP is pretending file maxTime is now+4hours."
    }


    /**
     * This test &amp;distinct().
     *
     * @throws Throwable if trouble
     */
    public static void testDistinct() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testDistinct() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //time constraints force erddap to get actual data, (not just station variables)
        //  and order of variables says to sort by lon first
        userDapQuery = "longitude,latitude,station&station=~\"5.*\"&time>=2008-03-11&time<2008-03-12&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //2011-04-12 changed with reprocessing. Mostly 2 to mostly 3 decimal digits
"longitude,latitude,station\n" +
"degrees_east,degrees_north,\n" +
"-162.279,23.445,51001\n" +
"-162.058,24.321,51101\n" +
"-160.66,19.087,51003\n" +
"-158.116,21.673,51201\n" +
"-157.808,17.094,51002\n" +
"-157.668,21.417,51202\n" +
"-157.01,20.788,51203\n" +
"-153.913,0.0,51028\n" +
"-152.382,17.525,51004\n" +
"144.789,13.354,52200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //if no time constraint, erddap can use SUBSET_FILENAME
        String2.log("\n* now testing just subset variables");
        userDapQuery = "longitude,latitude,station&station=~\"5.*\"&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts2", ".nc");  //nc so test metadata
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = //note sorted by lon first (because of &distinct()), not order in subset file
           //2011-04-12 lots of small changes due to full reprocessing
"netcdf EDDTableFromNcFiles_distincts2.nc {\n" +
" dimensions:\n" +
"   row = 20;\n" +
"   stationStringLength = 5;\n" +
" variables:\n" +
"   float longitude(row=20);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -162.279f, 171.395f; // float\n" +
"     :axis = \"X\";\n" +
"     :comment = \"The longitude of the station.\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float latitude(row=20);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 0.0f, 24.321f; // float\n" +
"     :axis = \"Y\";\n" +
"     :comment = \"The latitude of the station.\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   char station(row=20, stationStringLength=5);\n" +
"     :cf_role = \"timeseries_id\";\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Station Name\";\n" +
"\n" +
" :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :cdm_data_type = \"TimeSeries\";\n" +
" :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
" :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :contributor_role = \"Source of data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfeg.noaa.gov\";\n" +
" :Easternmost_Easting = 171.395f; // float\n" +
" :featureType = \"TimeSeries\";\n" +
" :geospatial_lat_max = 24.321f; // float\n" +
" :geospatial_lat_min = 0.0f; // float\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = 171.395f; // float\n" +
" :geospatial_lon_min = -162.279f; // float\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_positive = \"down\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"NOAA NDBC\n" +
today + " http://www.ndbc.noaa.gov/\n" +
today + " http://127.0.0.1:8080?station,longitude,latitude&distinct()\";\n" +
" :id = \"subset\";\n" +
" :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :institution = \"NOAA NDBC, CoastWatch WCN\";\n" +
" :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, altitude, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
" :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
" :Northernmost_Northing = 24.321f; // float\n" +
" :observationDimension = \"row\";\n" +
" :project = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :quality = \"Automated QC checks with periodic manual QC\";\n" +
" :source = \"station observation\";\n" +
" :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :Southernmost_Northing = 0.0f; // float\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"station, longitude, latitude\";\n" +
" :summary = \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data (quality controlled, before\n" +
"2012-03-01T00:00:00Z) and near real time data (less quality controlled, from\n" + //changes
"2012-03-01T00:00:00Z on).\";\n" +                                                 //changes  
" :time_coverage_resolution = \"P1H\";\n" +
" :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
" :Westernmost_Easting = -162.279f; // float\n" +
" data:\n" +
"longitude =\n" +
"  {-162.279, -162.058, -160.66, -158.303, -158.124, -158.116, -157.808, -157.668, -157.1, -157.01, -156.93, -156.427, -156.1, -154.056, -153.913, -153.9, -152.382, -144.668, 144.789, 171.395}\n" +
"latitude =\n" +
"  {23.445, 24.321, 19.087, 21.096, 21.281, 21.673, 17.094, 21.417, 20.4, 20.788, 21.35, 21.019, 20.4, 23.546, 0.0, 23.558, 17.525, 13.729, 13.354, 7.092}\n" +
"station =\"51001\", \"51101\", \"51003\", \"51200\", \"51204\", \"51201\", \"51002\", \"51202\", \"51027\", \"51203\", \"51026\", \"51205\", \"51005\", \"51000\", \"51028\", \"51100\", \"51004\", \"52009\", \"52200\", \"52201\"\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //if just one var, erddap can use DISTINCT_SUBSET_FILENAME
        String2.log("\n* now testing just distinct subset variables");
        userDapQuery = "longitude&longitude>-154&longitude<-153&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts3", ".nc"); //nc so test metadata
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf EDDTableFromNcFiles_distincts3.nc {\n" +
" dimensions:\n" +
"   row = 3;\n" +
" variables:\n" +
"   float longitude(row=3);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -153.913f, -153.348f; // float\n" +
"     :axis = \"X\";\n" +
"     :comment = \"The longitude of the station.\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"\n" +
" :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :cdm_data_type = \"TimeSeries\";\n" +
" :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
" :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :contributor_role = \"Source of data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfeg.noaa.gov\";\n" +
" :Easternmost_Easting = -153.348f; // float\n" +
" :featureType = \"TimeSeries\";\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = -153.348f; // float\n" +
" :geospatial_lon_min = -153.913f; // float\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_positive = \"down\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"NOAA NDBC\";\n" +
" :id = \"EDDTableFromNcFiles_distincts3\";\n" +
" :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :institution = \"NOAA NDBC, CoastWatch WCN\";\n" +
" :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, altitude, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
" :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
" :observationDimension = \"row\";\n" +
" :project = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :quality = \"Automated QC checks with periodic manual QC\";\n" +
" :source = \"station observation\";\n" +
" :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"station, longitude, latitude\";\n" +
" :summary = \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data (quality controlled, before\n" +
"2012-03-01T00:00:00Z) and near real time data (less quality controlled, from\n" + //changes
"2012-03-01T00:00:00Z on).\";\n" +                                                 //changes
" :time_coverage_resolution = \"P1H\";\n" +
" :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
" :Westernmost_Easting = -153.913f; // float\n" +
" data:\n" +
"longitude =\n" +
"  {-153.913, -153.9, -153.348}\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }


    /** This test getting just station ids. */
    public static void testId() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testId() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        userDapQuery = "station&station>\"5\"&station<\"6\""; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_id", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"station\n" +
"\n" +
"51000\n" +
"51001\n" +
"51002\n" +
"51003\n" +
"51004\n" +
"51005\n" +
"51026\n" +
"51027\n" +
"51028\n" +
"51100\n" +
"51101\n" +
"51200\n" +
"51201\n" +
"51202\n" +
"51203\n" +
"51204\n" +
"51205\n" + //added 2012-03-25
"52009\n" +
"52200\n" +
"52201\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }
     
    /**
     * This tests orderBy.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderBy() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderBy() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderBy(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ob", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T22:00:00Z,51002,25.2,25.4\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T22:00:00Z,51003,25.4,24.3\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T22:00:00Z,51004,25.0,23.8\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T22:00:00Z,51028,27.8,27.1\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T22:00:00Z,51201,24.9,NaN\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T22:00:00Z,51202,24.6,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderBy(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ob2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T22:00:00Z,51002,25.2,25.4\n" +
"2005-04-19T22:00:00Z,51003,25.4,24.3\n" +
"2005-04-19T22:00:00Z,51004,25.0,23.8\n" +
"2005-04-19T22:00:00Z,51028,27.8,27.1\n" +
"2005-04-19T22:00:00Z,51201,24.9,NaN\n" +
"2005-04-19T22:00:00Z,51202,24.6,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests orderByMax.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderByMax() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderBy() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMax(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obm", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMax(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obm2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests station,lon,lat.
     *
     * @throws Throwable if trouble
     */
    public static void testStationLonLat() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testStationLonLat() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "station,longitude,latitude&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_sll", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"station,longitude,latitude\n" +
",degrees_east,degrees_north\n" +
"23020,38.5,22.162\n" +
"31201,-48.134,-27.705\n" +
"32012,-85.384,-19.616\n" +
"32301,-105.2,-9.9\n" +
"32302,-85.1,-18.0\n" +
"32487,-77.737,3.517\n" +
"32488,-77.511,6.258\n" +
"41001,-72.698,34.675\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    }

    /**
     * This tests station,lon,lat.
     *
     * @throws Throwable if trouble
     */
    public static void testStationLonLat2() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testStationLonLat2() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("erdCalcofiSur"); 

        //.csv
        userDapQuery = "line_station,line,station,longitude,latitude&longitude=-143.38333&latitude=34.88333&longitude>=-180.0&longitude<=-100.0&latitude>=-5.0&latitude<=75.0";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_sll2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"line_station,line,station,longitude,latitude\n" +
",,,degrees_east,degrees_north\n" +
"034_282,34.0,282.0,-143.38333,34.88333\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }


    public static void testCalcofi() throws Throwable {
        testVerboseOn();
        String name, baseName, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int epo;

        //calcofi Subsurface
        EDDTable csub = (EDDTableFromNcFiles)oneFromDatasetXml("erdCalcofiSub"); 
        baseName = csub.className() + "csub";
        String csubDapQuery = "&longitude=-106.11667";

        //min max
        edv = csub.findDataVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -164.0833, ""); 
        Test.ensureEqual(edv.destinationMax(), -106.1167, "");

        tName = csub.makeNewFileForDapQuery(null, null, csubDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"line_station,line,station,longitude,latitude,time,altitude,chlorophyll,dark,light_percent,NH3,NO2,NO3,oxygen,PO4,pressure,primprod,salinity,silicate,temperature\n" +
",,,degrees_east,degrees_north,UTC,m,mg m-3,mg m-3 experiment-1,mg m-3 experiment-1,ugram-atoms L-1,ugram-atoms L-1,ugram-atoms L-1,mL L-1,ugram-atoms L-1,dbar,mg m-3 experiment-1,PSU,ugram-atoms L-1,degree_C\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,0.0,NaN,NaN,NaN,NaN,NaN,NaN,4.53,NaN,NaN,NaN,34.38,NaN,27.74\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-10.0,NaN,NaN,NaN,NaN,NaN,NaN,4.82,NaN,NaN,NaN,34.39,NaN,27.5\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-29.0,NaN,NaN,NaN,NaN,NaN,NaN,4.16,NaN,NaN,NaN,34.35,NaN,26.11\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-48.0,NaN,NaN,NaN,NaN,NaN,NaN,3.12,NaN,NaN,NaN,34.43,NaN,22.64\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-71.0,NaN,NaN,NaN,NaN,NaN,NaN,0.34,NaN,NaN,NaN,34.63,NaN,17.04\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-94.0,NaN,NaN,NaN,NaN,NaN,NaN,0.2,NaN,NaN,NaN,34.74,NaN,14.89\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-118.0,NaN,NaN,NaN,NaN,NaN,NaN,0.3,NaN,NaN,NaN,34.76,NaN,13.69\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-155.0,NaN,NaN,NaN,NaN,NaN,NaN,0.21,NaN,NaN,NaN,34.79,NaN,12.51\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-193.0,NaN,NaN,NaN,NaN,NaN,NaN,0.24,NaN,NaN,NaN,34.79,NaN,11.98\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-239.0,NaN,NaN,NaN,NaN,NaN,NaN,0.35,NaN,NaN,NaN,34.76,NaN,11.8\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-286.0,NaN,NaN,NaN,NaN,NaN,NaN,0.19,NaN,NaN,NaN,34.76,NaN,11.42\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //calcofi Surface
        EDDTable csur = (EDDTableFromNcFiles)oneFromDatasetXml("erdCalcofiSur"); 
        baseName = csur.className() + "csur";
        String csurDapQuery = "&longitude=-106.1167";

        //min max
        edv = csur.findDataVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -164.0833, "");
        Test.ensureEqual(edv.destinationMax(), -106.1167, "");

        tName = csur.makeNewFileForDapQuery(null, null, csurDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"line_station,line,station,longitude,latitude,time,altitude,air_pressure,chlorophyll,phaeopigment,primary_productivity,secchi_depth,wind_direction,wind_speed\n" +
",,,degrees_east,degrees_north,UTC,m,millibars,mg m-2,mg m-2,mg m-2,m,wmo 0877,knots\n" +
"171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,0.0,NaN,NaN,NaN,NaN,NaN,30,5\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    } //end of testCalcofi


    /** This tests converting global metadata into data. */
    public static void testGlobal() throws Throwable {
        testVerboseOn();
        String name, baseName, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int epo;

        //variant of calcofi Subsurface (has additional ID from global:id)
        EDDTable csub = (EDDTableFromNcFiles)oneFromDatasetXml("testGlobal"); 
        baseName = csub.className() + "Global";
        String csubDapQuery = "&longitude=-106.11667";

        //min max
        edv = csub.findDataVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -164.0833, "");
        Test.ensureEqual(edv.destinationMax(), -106.1167, "");

        tName = csub.makeNewFileForDapQuery(null, null, csubDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"ID,line_station,line,station,longitude,latitude,time,altitude,chlorophyll,dark,light_percent,NH3,NO2,NO3,oxygen,PO4,pressure,primprod,salinity,silicate,temperature\n" +
",,,,degrees_east,degrees_north,UTC,m,mg m-3,mg m-3 experiment-1,mg m-3 experiment-1,ugram-atoms L-1,ugram-atoms L-1,ugram-atoms L-1,mL L-1,ugram-atoms L-1,dbar,mg m-3 experiment-1,PSU,ugram-atoms L-1,degree_C\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,0.0,NaN,NaN,NaN,NaN,NaN,NaN,4.53,NaN,NaN,NaN,34.38,NaN,27.74\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-10.0,NaN,NaN,NaN,NaN,NaN,NaN,4.82,NaN,NaN,NaN,34.39,NaN,27.5\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-29.0,NaN,NaN,NaN,NaN,NaN,NaN,4.16,NaN,NaN,NaN,34.35,NaN,26.11\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-48.0,NaN,NaN,NaN,NaN,NaN,NaN,3.12,NaN,NaN,NaN,34.43,NaN,22.64\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-71.0,NaN,NaN,NaN,NaN,NaN,NaN,0.34,NaN,NaN,NaN,34.63,NaN,17.04\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-94.0,NaN,NaN,NaN,NaN,NaN,NaN,0.2,NaN,NaN,NaN,34.74,NaN,14.89\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-118.0,NaN,NaN,NaN,NaN,NaN,NaN,0.3,NaN,NaN,NaN,34.76,NaN,13.69\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-155.0,NaN,NaN,NaN,NaN,NaN,NaN,0.21,NaN,NaN,NaN,34.79,NaN,12.51\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-193.0,NaN,NaN,NaN,NaN,NaN,NaN,0.24,NaN,NaN,NaN,34.79,NaN,11.98\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-239.0,NaN,NaN,NaN,NaN,NaN,NaN,0.35,NaN,NaN,NaN,34.76,NaN,11.8\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,-286.0,NaN,NaN,NaN,NaN,NaN,NaN,0.19,NaN,NaN,NaN,34.76,NaN,11.42\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


    } //end of testCalcofi


    public static void testGenerateBreakUpPostDatasetsXml() throws Throwable {
            //String tFileDir, String tFileNameRegex, String sampleFileName, 
            //int tReloadEveryNMinutes,
            //String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
            //String tColumnNameForExtract, String tSortedColumnSourceName,
            //String tSortFilesBySourceNames, 
            //String tInfoUrl, String tInstitution, String tSummary, String tTitle,
            //Attributes externalAddGlobalAttributes) 
        String2.log(generateDatasetsXml(
            "F:/u00/cwatch/erddap2/copy/tcPostDet3/",
            ".*\\.nc", 
            "F:/u00/cwatch/erddap2/copy/tcPostDet3/Barbarax20Block/LAMNAx20DITROPIS/Nx2fA.nc",
            "",
            100000000, 
            "", "", "", "", "unique_tag_id",
            "PI, scientific_name, stock", 
            "", "", "", "",
            new Attributes()));
    }


    /** NOT FOR GENERAL USE. Bob uses this to manage the GTSPP dataset. 
     * 
     * @param isoStartTime  e.g., "2010-06-11T00:00:00Z". Only files which are newer
     *     than this will be unzipped.
     */
/* don't use this. see bobConsolidateGtspp
    public static void bobUnzipGtspp(String isoStartTime) {
        String sourceDir = "f:/data/gtspp/bestNcZip/";
        String destDir   = "f:/data/gtspp/bestNcIndividual/";

        //get the names of all of the .zip files
        String2.log("\n*** EDDTableFromNcFiles.bobUnzipGtspp");
        long time = System.currentTimeMillis();
        String names[] = RegexFilenameFilter.fullNameList(sourceDir, ".*\\.zip");
        String2.log(names.length + " .zip files found in " + sourceDir);

        //go through the files
        double startTimeMillis = Calendar2.isoStringToEpochSeconds(isoStartTime) * 1000.0;
        StringBuilder tErrorSB = new StringBuilder();
        int nSkip = 0;
        int nSuccess = 0;
        int nFail = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                //if it is older than startTimeMillis, skip it
                long lastMod = File2.getLastModified(names[i]);
                if (lastMod < startTimeMillis) {
                    nSkip++;
                    continue;
                }

                //unzip it
                String2.log("unzipping #" + i + " of " + names.length + ": " + names[i]);
                SSR.unzip(names[i], destDir, false, 100); //ignoreZipDirectories, timeOutSeconds
                nSuccess++;
            
            } catch (Throwable t) {
                nFail++;
                String tError = "ERROR while unzipping " + names[i] + "\n" + 
                    MustBe.throwableToString(t) + "\n";
                String2.log(tError);
                tErrorSB.append(tError + "\n");
            }
        }

        //done!
        String2.log("\n\nCumulative errors:\n");
        String2.log(tErrorSB.toString());
        String2.log(
            "*** bobUnzipGtspp finished. nSkip=" + nSkip + " nSuccess=" + nSuccess + 
            " nFail=" + nFail + " time=" + (System.currentTimeMillis() - time) + "\n");
    }
*/
    /** NOT FOR GENERAL USE. Bob used this to generate the GTSPP datasets.xml content.
     */
    public static void bobGenerateGtsppDatasetsXml() throws Throwable {
        String2.log(EDDTableFromNcFiles.generateDatasetsXml(
        "f:/data/gtspp/bestNcConsolidated/", ".*\\.nc", 
        "f:/data/gtspp/bestNcConsolidated/2010/01/2010-01_0E_-60N.nc.nc", 
        "",
        10080, //reloadMinutes
        "", "\\.nc", ".*", //tPreExtractRegex, tPostExtractRegex, tExtractRegex
        "station_id", "depth", //tColumnNameForExtract, tSortedColumnSourceName
        "time station_id", //tSortFilesBySourceNames
        "http://www.nodc.noaa.gov/GTSPP/", "NOAA NODC", //tInfoUrl, tInstitution        
        "put the summary here", //summary
        "Global Temperature-Salinity Profile Program", //tTitle
        new Attributes())); //externalAddGlobalAttributes) 
    }

    /** NOT FOR GENERAL USE. Bob uses this to consolidate the individual GTSPP
     * data files into 30� x 30� x 1 month files (tiles).
     * 30� x 30� leads to 12x6=72 files for a given time point, so a request
     * for a short time but entire world opens ~72 files.
     * There are ~240 months worth of data, so a request for a small lon lat 
     * range for all time opens ~240 files.
     *
     * <p>Why tile? Because there are ~10^6 profiles/year now, so ~10^7 total.
     * And if 100 bytes of info per file for EDDTableFromFiles fileTable, that's 1 GB!.
     * So there needs to be fewer files.
     * We want to balance number of files for 1 time point (all region tiles), 
     * and number of time point files (I'll stick with their use of 1 month).
     * The tiling size selected is ok, but searches for single profile (by name)
     * are slow since a given file may have a wide range of station_ids.
     *
     * <p>Quality flags
     * <br>http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf
     * <br>http://www.ifremer.fr/gosud/formats/gtspp_qcflags.htm
     * <br>CODE  SIGNIFICATION
     * <br>0     NOT CONTROLLED VALUE
     * <br>1     CORRECT VALUE
     * <br>2     VALUE INCONSISTENT WITH STATISTICS
     * <br>3     DOUBTFUL VALUE (spike, ...)
     * <br>4     FALSE VALUE (out of scale, constant profile, vertical instability, ...)
     * <br>5     VALUE MODIFIED DURING QC (only for interpolate location or date)
     * <br>6-8   Not USED
     * <br>9     NO VALUE
     * <br>
     * <br>I interpret as: okay values are 1, 2, 5
     *
     * @param firstYear  e.g., 1990
     * @param firstMonth e.g., 1  (1..)
     * @param lastYear  e.g., 2010
     * @param lastMonth e.g., 12  (1..)     
     * @param testMode if true, this just processes .nc files 
     *    already in testTempDir f:/data/gtspp/testTemp/
     *    and puts results in testDestDir f:/data/gtspp/testDest/.
     *    So the first/last/Year/Month params are ignored.
     */
    public static void bobConsolidateGtsppTgz(int firstYear, int firstMonth,
        int lastYear, int lastMonth, boolean testMode) throws Throwable {

        int chunkSize = 30;  //lon width, lat height of a tile, in degrees
        int minLat = -90;
        int maxLat = 90;
        int minLon = -180;
        int maxLon = 180;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String zipDir      = "f:\\data\\gtspp\\bestNcZip\\"; //gtspp_at199001.tgz
        String destDir     = "f:\\data\\gtspp\\bestNcConsolidated\\";
        String tempDir     = "f:\\data\\gtspp\\temp\\"; 
        String testTempDir = "f:\\data\\gtspp\\testTemp\\"; 
        String testDestDir = "f:\\data\\gtspp\\testDest\\";
        String logFile     = "f:\\data\\gtspp\\log" + 
            String2.replaceAll(today, "-", "") + ".txt"; 
        File2.makeDirectory(tempDir);
        //http://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm
        //1=correct, 2=probably correct, 5=modified (so now correct)
        //pre 2012-04-15 was {1,2,5}
        //pre 2012-05-25 was {1,2}
        int okQF[] = {1,2,5}; 
        String okQFCsv = String2.toCSSVString(okQF);
        float depthMV       = 99999; //was -99;
        float temperatureMV = 99999; //was -99;
        float salinityMV    = 99999; //was -99;
        int qMV = 9;
        String timeUnits = "days since 1900-01-01 00:00:00"; //causes roundoff error(!)
        double timeBaseAndFactor[] = Calendar2.getTimeBaseAndFactor(timeUnits);
        //impossible values:
        float minDepth       = 0,  maxDepth = 10000;
        float minTemperature = -4, maxTemperature = 40;
        float minSalinity    = 0,  maxSalinity = 41;


        if (testMode) {
            firstYear = 1990; firstMonth = 1;
            lastYear = 1990; lastMonth = 1;
        }

        SSR.verbose = false;
        
        String2.setupLog(true, false, 
            logFile, false, false, Integer.MAX_VALUE);

        String2.log("*** bobConsolidateGtsppTgz");
        long elapsedTime = System.currentTimeMillis();
        //q_pos (position quality flag), q_date_time (time quality flag)
        int stationCol = -1, organizationCol = -1, dataTypeCol = -1, 
            platformCol = -1, cruiseCol = -1,
            longitudeCol = -1, latitudeCol = -1, timeCol = -1, 
            depthCol = -1, temperatureCol = -1, salinityCol = -1;
        int totalNGoodStation = 0, totalNGoodPos = 0, totalNGoodTime = 0, 
            totalNGoodDepth = 0, totalNGoodTemperature = 0, totalNGoodSalinity = 0;
        int totalNBadStation = 0, totalNBadPos = 0, totalNBadTime = 0, 
            totalNBadDepth = 0, totalNBadTemperature = 0, totalNBadSalinity = 0,
            totalNWarnings = 0, totalNExceptions = 0;
        long totalNGoodRows = 0, totalNBadRows = 0;
        StringArray impossibleNanLat = new StringArray();
        StringArray impossibleMinLat = new StringArray();
        StringArray impossibleMaxLat = new StringArray();
        StringArray impossibleNanLon = new StringArray();
        StringArray impossibleMinLon = new StringArray();
        StringArray impossibleMaxLon = new StringArray();
        //StringArray impossibleNaNDepth = new StringArray();
        StringArray impossibleMinDepth = new StringArray();
        StringArray impossibleMaxDepth = new StringArray();
        //StringArray impossibleNanTemperature = new StringArray();
        StringArray impossibleMinTemperature = new StringArray();
        StringArray impossibleMaxTemperature = new StringArray();
        //StringArray impossibleNanSalinity = new StringArray();
        StringArray impossibleMinSalinity = new StringArray();
        StringArray impossibleMaxSalinity = new StringArray();
        int nLons = 0, nLats = 0, nFiles = 0;
        int lonSum = 0, latSum = 0;
        long profilesSum = 0;
        long rowsSum = 0;


        //*** process a month's data
        int year = firstYear;
        int month = firstMonth;
        long chunkTime = System.currentTimeMillis();
        while (year <= lastYear) {
            String2.log("\n*** " + Calendar2.getCurrentISODateTimeStringLocal() +
                " start processing year=" + year + " month=" + month);

            String zMonth  = String2.zeroPad("" + month,       2);
            String zMonth1 = String2.zeroPad("" + (month + 1), 2);
            double minEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth  + "-01");
            double maxEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth1 + "-01");                   

            //destination directory
            String tDestDir = testMode? testDestDir : destDir + year + "\\" + zMonth + "\\";
            File2.makeDirectory(tDestDir);
            HashMap tableHashMap = new HashMap();
            //make sure all files are deleted 
            int waitSeconds = 2;
            int nAttempts = 10;
            long cmdTime = System.currentTimeMillis();
            String cmd = "del/q " + tDestDir + "*.*"; 
            for (int attempt = 0; attempt < nAttempts; attempt++) {
                if (attempt % 8 == 0) {
                    String2.log(cmd);
                    SSR.dosShell(cmd, 30*60); //10 minutes*60 seconds
                    //File2.deleteAllFiles(tempDir);  //previous method
                }
                Math2.gc(waitSeconds * 1000); //good time to gc
                File destDirFile = new File(tDestDir);
                File files[] = destDirFile.listFiles();
                String2.log("  nRemainingFiles=" + files.length);
                if (files.length == 0)
                    break;
                waitSeconds = 2 * nAttempts;
            }
            String2.log("  cmd total time=" + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));


            //unzip all atlantic, indian, and pacific .zip files for that month 
            String region2[] = {"at", "in", "pa"};
            int nRegions = testMode? 1 : 3;
            for (int region = 0; region < nRegions; region++) {
                String sourceBaseName = "gtspp4_" + region2[region] + year + zMonth;
                String sourceZipJustFileName = sourceBaseName + ".tgz";
                String sourceZipName = zipDir + sourceZipJustFileName;

                if (!testMode) {

                    //delete all files in tempDir
                    waitSeconds = 2;
                    nAttempts = 10;
                    cmdTime = System.currentTimeMillis();
                    cmd = "del/q " + tempDir + "*.*"; 
                    String2.log(""); //blank line
                    for (int attempt = 0; attempt < nAttempts; attempt++) {
                        String2.log(cmd);
                        SSR.dosShell(cmd, 30*60); //10 minutes*60 seconds
                        //File2.deleteAllFiles(tempDir);  //previous method

                        //delete dirs too
                        File2.deleteAllFiles(tempDir, true, true);
 
                        Math2.gc(waitSeconds * 1000); //good time to gc
                        File tempDirFile = new File(tempDir);
                        File files[] = tempDirFile.listFiles();
                        String2.log("  nRemainingFiles=" + files.length);
                        if (files.length == 0)
                            break;
                        waitSeconds = 2 * nAttempts;
                    }
                    String2.log("  cmd total time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

                    //unzip file into tempDir         //gtspp_at199001.zip
                    cmd = "c:\\programs\\7-Zip\\7z -y e " + sourceZipName + " -o" + tempDir + " -r"; 
                    cmdTime = System.currentTimeMillis();
                    String2.log("\n*** " + cmd);
                    SSR.dosShell(cmd, 30*60); //10 minutes*60 seconds
                    String2.log("  cmd time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

                    //extract from the .tar file   //gtspp4_at199001.tar
                    cmd = "c:\\programs\\7-Zip\\7z -y e " + tempDir + sourceBaseName + ".tar -o" + tempDir + " -r"; 
                    cmdTime = System.currentTimeMillis();
                    String2.log("\n*** " + cmd);
                    SSR.dosShell(cmd, 120*60); //120 minutes*60 seconds
                    String2.log("  cmd time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));
                    
                    //previous method
                    //SSR.unzip(sourceZipName,
                    //    tempDir, true, 100 * 60); //ignoreZipDirectories, timeOutSeconds 100 minutes
                }

                //read each file and put data in proper table
                String tTempDir = testMode? testTempDir : tempDir;
                File tTempDirAsFile = new File(tTempDir);
                String sourceFileNames[] = tTempDirAsFile.list(); //just the file names
                String2.log("\nunzipped " + sourceFileNames.length + " files");
                int nSourceFileNames = //testMode? 100 : 
                    sourceFileNames.length;
                int nGoodStation = 0, nGoodPos = 0, nGoodTime = 0, 
                    nGoodDepth = 0, nGoodTemperature = 0, nGoodSalinity = 0, nGoodRows = 0;
                int nBadStation = 0, nBadPos = 0, nBadTime = 0, 
                    nBadDepth = 0, nBadTemperature = 0, nBadSalinity = 0, nBadRows = 0,
                    nWarnings = 0, nExceptions = 0;
                long fileReadTime = System.currentTimeMillis();
                profilesSum += nSourceFileNames;
                for (int sfi = 0; sfi < nSourceFileNames; sfi++) {
                    String sourceFileName = sourceFileNames[sfi];
                    if (sfi % 10000 == 0) {
                        if (sfi > 0) 
                            Math2.gc(3 * 1000); //good time to gc
                        //high water mark is ~160 MB, so memory not a problem
                        String2.log("file #" + sfi + " " + Math2.memoryString());
                    }

                    if (!sourceFileName.endsWith(".nc")) {
                        //String2.log("ERROR: not a .nc file: " + sourceFileName);
                        continue;
                    }

                    NetcdfFile ncFile = null; 

                    try {
                        //get the station name
                        //gtspp_13635162_te_111.nc  gtspp_10313692_cu_111.nc
                        if (!sourceFileName.matches("gtspp_[0-9]+_.*\\.nc")) { //was "\\d+")) {//all digits
                            nBadStation++;
                            throw new SimpleException("Invalid sourceFileName=" + sourceFileName);
                        }
                        int po = sourceFileName.indexOf('_', 6);
                        if (po < 0) {
                            nBadStation++;
                            throw new SimpleException("Invalid sourceFileName=" + sourceFileName);
                        }
                        int station = String2.parseInt(sourceFileName.substring(6, po));
                        nGoodStation++;
                        String key = sourceZipJustFileName + " " + sourceFileName;

                        //open the file
                        ncFile = NcHelper.openFile(tTempDir + sourceFileName);
                        Variable var;
                        Attributes tVarAtts = new Attributes();
                        String tUnits;

                        //get all of the data 

                        //stream_ident
                        var = ncFile.findVariable("stream_ident");                        
                        String organization = "";
                        String dataType = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No stream_ident in " + sourceFileName);
                        } else {
                            PrimitiveArray streamPA = NcHelper.getPrimitiveArray(var);
                            if (streamPA instanceof StringArray && streamPA.size() > 0) {
                                String stream = streamPA.getString(0);
                                if (stream.length() >= 4) {
                                    organization = stream.substring(0, 2).trim();
                                    dataType = stream.substring(2, 4).trim();
                                } else {
                                    String2.log("WARNING: stream_ident isn't a 4 char string: " + stream);
                                }
                            } else {
                                String2.log("WARNING: stream_ident isn't a StringArray: " + 
                                    streamPA.toString());
                            }
                        }

                        //platform_code
                        var = ncFile.findVariable("gtspp_platform_code");                        
                        String platform = "";
                        if (var == null) {
                            //a small percentage have this problem
                            //nWarnings++;
                            //String2.log("WARNING: No gtspp_platform_code in " + sourceFileName);
                        } else {
                            PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
                            if (pa instanceof StringArray && pa.size() > 0) {
                                platform = pa.getString(0).trim();
                                //String2.log("platform_code=" + platform_code);
                            } else {
                                String2.log("WARNING: gtspp_platform_code isn't a StringArray: " + 
                                    pa.toString());
                            }
                        }

                        //cruise
                        var = ncFile.findVariable("cruise_id");                        
                        String cruise = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No cruise_id in " + sourceFileName);
                        } else {
                            PrimitiveArray cruisePA = NcHelper.getPrimitiveArray(var);
                            if (cruisePA instanceof StringArray && cruisePA.size() > 0) {
                                cruise = cruisePA.getString(0).trim();
                            } else {
                                String2.log("WARNING: cruise_id isn't a StringArray: " + 
                                    cruisePA.toString());
                            }
                        }

                        //prof_type  is TEMP or PSAL so don't save it.
                        /*var = ncFile.findVariable("prof_type");                        
                        String prof_type = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No prof_type in " + sourceFileName);
                        } else {
                            PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
                            if (pa instanceof StringArray && pa.size() > 0) {
                                prof_type = pa.getString(0).trim();
                                String2.log("prof_type=" + prof_type);
                            } else {
                                String2.log("WARNING: prof_type isn't a StringArray: " + 
                                    pa.toString());
                            }
                        }*/

                        //position quality flag 
                        var = ncFile.findVariable("position_quality_flag"); //was "q_pos");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No position_quality_flag in " + sourceFileName);
                        } else {
                            PrimitiveArray q_pos = NcHelper.getPrimitiveArray(var);
                            if (!(q_pos instanceof IntArray) || q_pos.size() != 1) 
                                throw new SimpleException("Invalid position_quality_flag=" + q_pos);
                            int ti = q_pos.getInt(0);
                            if (String2.indexOf(okQF, ti) < 0) {
                                nBadPos++;
                                continue;
                            }
                            //nGoodPos++; is below
                        }

                        //time quality flag 
                        var = ncFile.findVariable("time_quality_flag"); //q_date_time");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No time_quality_flag in " + sourceFileName);
                        } else {
                            PrimitiveArray q_date_time = NcHelper.getPrimitiveArray(var);
                            if (!(q_date_time instanceof IntArray) || q_date_time.size() != 1) 
                                throw new SimpleException("Invalid time_quality_flag=" + q_date_time);
                            int ti = q_date_time.getInt(0);
                            if (String2.indexOf(okQF, ti) < 0) {
                                nBadTime++;
                                continue;
                            }
                            //nGoodTime is below
                        }

                        //time
                        var = ncFile.findVariable("time");                        
                        if (var == null) 
                            throw new SimpleException("No time!");
                        tVarAtts.clear();
                        NcHelper.getVariableAttributes(var, tVarAtts);
                        tUnits = tVarAtts.getString("units");
                        if (!timeUnits.equals(tUnits)) 
                            throw new SimpleException("Invalid time units=" + tUnits);
                        PrimitiveArray time = NcHelper.getPrimitiveArray(var);
                        if (!(time instanceof DoubleArray) || time.size() != 1) 
                            throw new SimpleException("Invalid time=" + time);
                        double tTime = Calendar2.unitsSinceToEpochSeconds(
                            timeBaseAndFactor[0], timeBaseAndFactor[1], time.getDouble(0));
                        String isoTime = Calendar2.safeEpochSecondsToIsoStringT(tTime, "");
                        if (tTime < minEpochSeconds || tTime > maxEpochSeconds) 
                            throw new SimpleException("Invalid tTime=" + isoTime);
                        //original times (that I looked at) are to nearest second
                        //so round to nearest second (fix .99999 problems)
                        tTime = Math.rint(tTime); 
                        nGoodTime++;

                        //longitude  (position qFlag is good)
                        var = ncFile.findVariable("longitude");                        
                        if (var == null) {
                            impossibleNanLon.add(key + " lon=null");
                            continue;
                        }
                        PrimitiveArray longitude = NcHelper.getPrimitiveArray(var);
                        if (!(longitude instanceof FloatArray) || longitude.size() != 1) {
                            impossibleNanLon.add(key + " lon=wrongTypeOrSize");
                            continue;
                        }
                        float lon = longitude.getFloat(0);
                        if (Float.isNaN(lon)) { 
                            impossibleNanLon.add(key + " lon=NaN");
                            continue;
                        } else if (lon < minLon) {
                            impossibleMinLon.add(key + " lon=" + lon);
                            //fall through
                        } else if (lon > maxLon) { 
                            impossibleMaxLon.add(key + " lon=" + lon);
                            //fall through
                        }
                        lon = (float)Math2.anglePM180(lon);

                        //latitude (position qFlag is good)
                        var = ncFile.findVariable("latitude");                        
                        if (var == null) {
                            impossibleNanLat.add(key + " lat=null");
                            continue;
                        }
                        PrimitiveArray latitude = NcHelper.getPrimitiveArray(var);
                        if (!(latitude instanceof FloatArray) || latitude.size() != 1) {
                            impossibleNanLat.add(key + " lat=wrongTypeOrSize");
                            continue;
                        }
                        float lat = latitude.getFloat(0);
                        if (Float.isNaN(lat)) { 
                            impossibleNanLat.add(key + " lat=NaN");
                            continue;
                        } else if (lat < minLat) {
                            impossibleMinLat.add(key + " lat=" + lat);
                            continue;
                        } else if (lat > maxLat) { 
                            impossibleMaxLat.add(key + " lat=" + lat);
                            continue;
                        }
                        nGoodPos++;

                        //depth
                        var = ncFile.findVariable("z");                        
                        if (var == null) 
                            throw new SimpleException("No z!");
                        PrimitiveArray depth = NcHelper.getPrimitiveArray(var);
                        if (!(depth instanceof FloatArray) || depth.size() == 0) 
                            throw new SimpleException("Invalid z=" + depth);
                        int nDepth = depth.size();

                        //DEPH_qparm
                        var = ncFile.findVariable("z_variable_quality_flag"); //DEPH_qparm");                        
                        if (var == null) 
                            throw new SimpleException("No z_variable_quality_flag!");
                        PrimitiveArray DEPH_qparm = NcHelper.getPrimitiveArray(var);
                        if (!(DEPH_qparm instanceof IntArray) || DEPH_qparm.size() != nDepth) 
                            throw new SimpleException("Invalid z_variable_quality_flag=" + DEPH_qparm);
                        //nGoodDepth is below

                        //temperature
                        var = ncFile.findVariable("temperature");                        
                        PrimitiveArray temperature;
                        PrimitiveArray TEMP_qparm;
                        float temperatureFV = temperatureMV;
                        if (var == null) {
                            //nWarnings++;
                            //String2.log("WARNING: No temperature in " + sourceFileName); reasonably common
                            temperature = PrimitiveArray.factory(float.class,  nDepth, "" + temperatureMV);
                            TEMP_qparm  = PrimitiveArray.factory(int.class,    nDepth, "" + qMV);
                        } else {            
                            temperature = NcHelper.getPrimitiveArray(var);
                            if (!(temperature instanceof FloatArray) || temperature.size() != nDepth) 
                                throw new SimpleException("Invalid temperature=" + temperature);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            temperatureFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(temperatureFV) && temperatureFV != temperatureMV)
                                throw new SimpleException("Invalid temperature _FillValue=" + temperatureFV);

                            //TEMP_qparm
                            var = ncFile.findVariable("temperature_quality_flag"); //TEMP_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No temperature_quality_flag in " + sourceFileName);
                                TEMP_qparm = PrimitiveArray.factory(int.class,  nDepth, "" + qMV);
                            } else {
                                TEMP_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(TEMP_qparm instanceof IntArray) || TEMP_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid temperature_quality_flag=" + TEMP_qparm);
                            }
                        }

                        //salinity
                        var = ncFile.findVariable("salinity");                        
                        PrimitiveArray salinity;
                        PrimitiveArray PSAL_qparm;
                        float salinityFV = salinityMV;
                        if (var == null) {
                            //String2.log("WARNING: No salinity in " + sourceFileName);   //very common
                            salinity   = PrimitiveArray.factory(float.class,  nDepth, "" + salinityMV);
                            PSAL_qparm = PrimitiveArray.factory(int.class,    nDepth, "" + qMV);
                        } else {
                            salinity = NcHelper.getPrimitiveArray(var);
                            if (!(salinity instanceof FloatArray) || salinity.size() != nDepth) 
                                throw new SimpleException("Invalid salinity=" + salinity);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            salinityFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(salinityFV) && salinityFV != salinityMV)
                                throw new SimpleException("Invalid salinity _FillValue=" + salinityFV);

                            //PSAL_qparm
                            var = ncFile.findVariable("salinity_quality_flag"); //PSAL_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No salinity_quality_flag in " + sourceFileName);
                                PSAL_qparm = PrimitiveArray.factory(int.class,  nDepth, "" + qMV);
                            } else {
                                PSAL_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(PSAL_qparm instanceof IntArray) || PSAL_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid salinity_quality_flag=" + PSAL_qparm);
                            }                   
                        }

                        //clean the data
                        //(good to do it here so memory usage is low -- table remains as small as possible)
                        //Change "impossible" data to NaN
                        //(from http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf
                        //pg 61 has Table 2.1: Global Impossible Parameter Values).
                        BitSet keep = new BitSet();
                        keep.set(0, nDepth);  //all true 

                        //find worst impossible depth/temperature/salinity for this station
                        //boolean tImpossibleNanDepth       = false;
                        //boolean tImpossibleNanTemperature = false;
                        //boolean tImpossibleNanSalinity    = false;
                        float tImpossibleMinDepth = minDepth;
                        float tImpossibleMaxDepth = maxDepth;
                        float tImpossibleMinTemperature = minTemperature;
                        float tImpossibleMaxTemperature = maxTemperature;
                        float tImpossibleMinSalinity = minSalinity;
                        float tImpossibleMaxSalinity = maxSalinity;


                        for (int row = 0; row < nDepth; row++) {

                            //DEPH_qparm
                            int qs = DEPH_qparm.getInt(row);
                            float f = depth.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (Float.isNaN(f) || f == depthMV) { //"impossible" depth
                                //tImpossibleNanDepth = true;
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f < minDepth) {
                                tImpossibleMinDepth = Math.min(tImpossibleMinDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f > maxDepth) { 
                                tImpossibleMaxDepth = Math.max(tImpossibleMaxDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            }
                            nGoodDepth++;

                            boolean hasData = false;

                            //temperature
                            qs = TEMP_qparm.getInt(row);
                            f = temperature.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                temperature.setString(row, "");  //so bad value is now NaN
                                nBadTemperature++;
                            } else if (Float.isNaN(f) || f == temperatureMV) {
                                temperature.setString(row, "");  //so missing value is now NaN
                                nBadTemperature++;
                            } else if (f < minTemperature) { //"impossible" water temperature
                                tImpossibleMinTemperature = Math.min(tImpossibleMinTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else if (f > maxTemperature) { //"impossible" water temperature
                                tImpossibleMaxTemperature = Math.max(tImpossibleMaxTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else {
                                nGoodTemperature++;
                                hasData = true;
                            }

                            //salinity
                            qs = PSAL_qparm.getInt(row);
                            f = salinity.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                salinity.setString(row, "");  //so bad value is now NaN
                                nBadSalinity++;
                            } else if (Float.isNaN(f) || f == salinityMV) {
                                salinity.setString(row, "");  //so missing value is now NaN
                                nBadSalinity++;
                            } else if (f < minSalinity) { //"impossible" salinity
                                tImpossibleMinSalinity = Math.min(tImpossibleMinSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else if (f > maxSalinity) { //"impossible" salinity
                                tImpossibleMaxSalinity = Math.max(tImpossibleMaxSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else {
                                nGoodSalinity++;
                                hasData = true;
                            }

                            //no valid temperature or salinity data?
                            if (!hasData) {           
                                keep.clear(row);
                            }
                        }

                        //ensure sizes still correct
                        Test.ensureEqual(depth.size(),       nDepth, "depth.size changed!");
                        Test.ensureEqual(temperature.size(), nDepth, "temperature.size changed!");
                        Test.ensureEqual(salinity.size(),    nDepth, "salinity.size changed!");

                        //actually remove the bad rows
                        int tnGood = keep.cardinality();
                        if (testMode && verbose) String2.log(sourceFileName + 
                            ": nGoodRows=" + tnGood + 
                            " nBadRows=" + (nDepth - tnGood));
                        nGoodRows += tnGood;
                        nBadRows += nDepth - tnGood;
                        depth.justKeep(keep);
                        temperature.justKeep(keep);
                        salinity.justKeep(keep);
                        nDepth = depth.size();

                        //impossible
                        //if (tImpossibleNanDepth)
                        //     impossibleNanDepth.add(key + " hasNaN=true");
                        //if (tImpossibleNanTemperature)
                        //     impossibleNanTemperature.add(key + " hasNaN=true");
                        //if (tImpossibleNanSalinity)
                        //     impossibleNanSalinity.add(key + " hasNaN=true");

                        if (tImpossibleMinDepth < minDepth)
                             impossibleMinDepth.add(key + " worst = " + tImpossibleMinDepth);
                        if (tImpossibleMaxDepth > maxDepth)
                             impossibleMaxDepth.add(key + " worst = " + tImpossibleMaxDepth);
                        if (tImpossibleMinTemperature < minTemperature)
                             impossibleMinTemperature.add(key + " worst = " + tImpossibleMinTemperature);
                        if (tImpossibleMaxTemperature > maxTemperature)
                             impossibleMaxTemperature.add(key + " worst = " + tImpossibleMaxTemperature);
                        if (tImpossibleMinSalinity < minSalinity)
                             impossibleMinSalinity.add(key + " worst = " + tImpossibleMinSalinity);
                        if (tImpossibleMaxSalinity > maxSalinity)
                             impossibleMaxSalinity.add(key + " worst = " + tImpossibleMaxSalinity);


                        //which table
                        if (tnGood == 0)
                            continue;
                        int loni = Math2.roundToInt(Math.floor((Math.min(lon, maxLon-0.1f) - minLon) / chunkSize));
                        int lati = Math2.roundToInt(Math.floor((Math.min(lat, maxLat-0.1f) - minLat) / chunkSize));
                        String outTableName = 
                            (minLon + loni * chunkSize) + "E_" + (minLat + lati * chunkSize) + "N";
                            //String2.replaceAll(cruise + "_" + organization + dataType, ' ', '_'); //too many: 3000+/month in 2011
                        Table tTable = (Table)tableHashMap.get(outTableName);
                            
                        if (tTable == null) {

                            Attributes ncGlobalAtts = new Attributes();
                            NcHelper.getGlobalAttributes(ncFile, ncGlobalAtts);
                            String tHistory = ncGlobalAtts.getString("history");
                            tHistory =  tHistory != null && tHistory.length() > 0?
                                tHistory + "\n" : "";

                            //make a table for this platform
                            tTable = new Table();
                            Attributes ga = tTable.globalAttributes();
                            String ack = "These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on " + 
                                today + " from http://www.nodc.noaa.gov/GTSPP/.";
                            ga.add("acknowledgment", ack);
                            ga.add("license", 
                                "These data are openly available to the public.  " +
                                "Please acknowledge the use of these data with:\n" +
                                ack + "\n\n" +
                                "[standard]");
                            ga.add("history", 
                                tHistory + 
                                ".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ (http://www.nodc.noaa.gov/GTSPP/)\n" +
                                today + " Most recent ingest, clean, and reformat at ERD (bob.simons at noaa.gov).");
                            ga.add("infoUrl",    "http://www.nodc.noaa.gov/GTSPP/");
                            ga.add("institution","NOAA NODC");
                            ga.add("title",      "Global Temperature and Salinity Profile Programme (GTSPP) Data");

                            String attName = "gtspp_ConventionVersion";
                            String attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_program";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_programVersion";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_handbook_version";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            organizationCol  = tTable.addColumn(tTable.nColumns(), "org",               new StringArray(),
                                new Attributes());
                            platformCol      = tTable.addColumn(tTable.nColumns(), "platform",          new StringArray(),
                                new Attributes());
                            dataTypeCol      = tTable.addColumn(tTable.nColumns(), "type",              new StringArray(),
                                new Attributes());
                            cruiseCol        = tTable.addColumn(tTable.nColumns(), "cruise",            new StringArray(),
                                new Attributes());
                            stationCol       = tTable.addColumn(tTable.nColumns(), "station_id",        new IntArray(),
                                new Attributes());
                            longitudeCol     = tTable.addColumn(tTable.nColumns(), "longitude",         new FloatArray(),
                                (new Attributes()).add("units", "degrees_east"));
                            latitudeCol      = tTable.addColumn(tTable.nColumns(), "latitude",          new FloatArray(),
                                (new Attributes()).add("units", "degrees_north"));
                            timeCol          = tTable.addColumn(tTable.nColumns(), "time",              new DoubleArray(),
                                (new Attributes()).add("units", EDV.TIME_UNITS));
                            depthCol         = tTable.addColumn(tTable.nColumns(), "depth",             new FloatArray(),
                                (new Attributes()).add("units", "m"));
                            temperatureCol   = tTable.addColumn(tTable.nColumns(), "temperature",       new FloatArray(),
                                (new Attributes()).add("units", "degree_C"));
                            salinityCol      = tTable.addColumn(tTable.nColumns(), "salinity",          new FloatArray(),
                                (new Attributes()).add("units", "PSU"));

                            tableHashMap.put(outTableName, tTable);
                        }

                        //put data in tTable
                        int oNRows = tTable.nRows();
                        ((StringArray)tTable.getColumn(organizationCol)).addN(nDepth, organization);
                        ((StringArray)tTable.getColumn(platformCol)).addN(nDepth, platform);
                        ((StringArray)tTable.getColumn(dataTypeCol)).addN(nDepth, dataType);
                        ((StringArray)tTable.getColumn(cruiseCol)).addN(nDepth, cruise);
                        ((IntArray   )tTable.getColumn(stationCol)).addN(nDepth, station);
                        ((FloatArray )tTable.getColumn(longitudeCol)).addN(nDepth, lon);
                        ((FloatArray )tTable.getColumn(latitudeCol)).addN(nDepth, lat);
                        ((DoubleArray)tTable.getColumn(timeCol)).addN(nDepth, tTime);
                        ((FloatArray )tTable.getColumn(depthCol)).append(depth);
                        ((FloatArray )tTable.getColumn(temperatureCol)).append(temperature);
                        ((FloatArray )tTable.getColumn(salinityCol)).append(salinity);

                        //ensure the table is valid (same size for each column)
                        tTable.ensureValid();

                    } catch (Throwable t) {
                        nExceptions++;
                        String2.log("ERROR while processing " + sourceFileName + "\n  " + 
                            MustBe.throwableToString(t));
                    } finally {
                        //always close the ncFile
                        if (ncFile != null) {
                            try {
                                ncFile.close(); 
                            } catch (Throwable t) {
                                String2.log("ERROR: unable to close " + sourceFileName + "\n" +
                                    MustBe.getShortErrorMessage(t));
                            }
                        }
                    }
                }

                String2.log("\n  time to read all those files = " + 
                    Calendar2.elapsedTimeString(System.currentTimeMillis() - fileReadTime));

                //end of region loop
                String2.log("\nIn zip=" + sourceZipName + 
                    "\n nExceptions=    " + nExceptions     + "        nWarnings="        + nWarnings +
                    "\n nBadStation=    " + nBadStation     + "        nGoodStation="     + nGoodStation +
                    "\n nBadPos=        " + nBadPos         + "        nGoodPos="         + nGoodPos +
                    "\n nBadTime=       " + nBadTime        + "        nGoodTime="        + nGoodTime +
                    "\n nBadDepth=      " + nBadDepth       + "        nGoodDepth="       + nGoodDepth +
                    "\n nBadTemperature=" + nBadTemperature + "        nGoodTemperature=" + nGoodTemperature +
                    "\n nBadSalinity=   " + nBadSalinity    + "        nGoodSalinity="    + nGoodSalinity);
                totalNGoodStation += nGoodStation;
                totalNGoodPos += nGoodPos;
                totalNGoodTime += nGoodTime;
                totalNGoodDepth += nGoodDepth; 
                totalNGoodTemperature += nGoodTemperature; 
                totalNGoodSalinity += nGoodSalinity;
                totalNGoodRows += nGoodRows;
                totalNBadPos += nBadPos; 
                totalNBadTime += nBadTime; 
                totalNBadDepth += nBadDepth; 
                totalNBadTemperature += nBadTemperature; 
                totalNBadSalinity += nBadSalinity;
                totalNBadRows += nBadRows;
                totalNWarnings += nWarnings;
                totalNExceptions += nExceptions;
            } //end of region loop

            //save by outTableName
            boolean filePrinted = false;
            Object keys[] = tableHashMap.keySet().toArray();
            int nKeys = keys.length;
            String2.log("\n*** saving nFiles=" + nKeys);
            for (int keyi = 0; keyi < nKeys; keyi++) {
                String key = keys[keyi].toString();
                Table tTable = (Table)tableHashMap.remove(key);
                if (tTable == null || tTable.nRows() == 0) {
                    String2.log("Unexpected: no table for key=" + key);
                    continue;
                }

                //sort by time, station, depth  
                //depth matches the source files: from surface to deepest
                tTable.sort(new int[]{timeCol, stationCol, depthCol}, 
                        new boolean[]{true,    true,       true});

                //is this saving a small lat lon range?
                double stationStats[] = tTable.getColumn(stationCol).calculateStats();
                //double lonStats[]     = tTable.getColumn(longitudeCol).calculateStats();
                //double latStats[]     = tTable.getColumn(latitudeCol).calculateStats();
                //nLats++;
                //double latRange = latStats[PrimitiveArray.STATS_MAX] - latStats[PrimitiveArray.STATS_MIN];
                //latSum += latRange;
                rowsSum += tTable.nRows();
                String2.log("    stationRange=" + Math2.roundToInt(stationStats[PrimitiveArray.STATS_MAX] - stationStats[PrimitiveArray.STATS_MIN]) +
                            //"  lonRange="     + Math2.roundToInt(lonStats[    PrimitiveArray.STATS_MAX] - lonStats[    PrimitiveArray.STATS_MIN]) +
                            //"  latRange="     + Math2.roundToInt(latRange) +
                              "  nRows="        + tTable.nRows());

                //save it
                String tName = tDestDir + 
                    String2.encodeFileNameSafe(key);
                /*if (lonStats[PrimitiveArray.STATS_MAX] > 45 &&
                    lonStats[PrimitiveArray.STATS_MIN] < -45) {

                    //NO MORE: This happened with 1 file/cruise, 
                    //  but won't happen now with lon/lat tiles.
                    //crosses dateline (or widely across lon=0)?  split into 2 files
                    Table ttTable = (Table)tTable.clone();
                    ttTable.oneStepApplyConstraint(0, "longitude", "<", "0");
                    ttTable.saveAsFlatNc(tName + "_W.nc", "row", false);
                    double lonStatsW[] = ttTable.getColumn(longitudeCol).calculateStats();
                    nLons++;
                    double lonRangeW = lonStatsW[PrimitiveArray.STATS_MAX] - lonStatsW[PrimitiveArray.STATS_MIN];
                    lonSum += lonRangeW;

                    ttTable = (Table)tTable.clone();
                    ttTable.oneStepApplyConstraint(0, "longitude", ">=", "0");
                    ttTable.saveAsFlatNc(tName + "_E.nc", "row", false);
                    double lonStatsE[] = ttTable.getColumn(longitudeCol).calculateStats();
                    nLons++;
                    double lonRangeE = lonStatsE[PrimitiveArray.STATS_MAX] - lonStatsE[PrimitiveArray.STATS_MIN];
                    lonSum += lonRangeE;
                    String2.log("  westLonRange=" + Math2.roundToInt(lonRangeW) +
                                "  eastLonRange=" + Math2.roundToInt(lonRangeE));
                } else */
                {
                    //nLons++;
                    nFiles++;
                    tTable.saveAsFlatNc(tName + ".nc",
                        "row", false); //convertToFakeMissingValues  (keep mv's as NaNs)
                }

                //print a file
                if (testMode && !filePrinted) {
                    filePrinted = true;
                    String2.log(NcHelper.dumpString(tName, true));
                }
            }
            String2.log("\ncumulative nProfiles=" + profilesSum + " nRows=" + rowsSum +
                " mean nRows/file=" + (rowsSum / Math.max(1, nFiles)));
            //if (nLats > 0) 
            //    String2.log(  "cumulative nLats=" + nLats + " meanLatRange=" + (float)(latSum / nLats));
            //if (nLons > 0) {
            //    String2.log(  "cumulative nLons=" + nLons + " meanLonRange=" + (float)(lonSum / nLons));
            //    String2.log("mean nRows per saved file = " + (rowsSum / nLons));
            //}

            //print list of impossible at end of year or end of run
            if (month == 12 || (year == lastYear && month == lastMonth)) {

                String2.log("\n*** " + Calendar2.getCurrentISODateTimeStringLocal() +
                        " bobConsolidateGtsppTgz finished the chunk ending " + 
                        year + "-" + month + "\n" +
                    "chunkTime=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - chunkTime));
                chunkTime = System.currentTimeMillis();

                //print impossible statistics
                String2.log("\nCumulative number of stations with:\n" +
                    "impossibleNanLon         = " + impossibleNanLon.size() + "\n" +
                    "impossibleMinLon         = " + impossibleMinLon.size() + "\n" +
                    "impossibleMaxLon         = " + impossibleMaxLon.size() + "\n" +
                    "impossibleNanLat         = " + impossibleNanLat.size() + "\n" +
                    "impossibleMinLat         = " + impossibleMinLat.size() + "\n" +
                    "impossibleMaxLat         = " + impossibleMaxLat.size() + "\n" +
                    "impossibleMinDepth       = " + impossibleMinDepth.size() + "\n" +
                    "impossibleMaxDepth       = " + impossibleMaxDepth.size() + "\n" +
                    //"impossibleLatLon      = " + impossibleLatLon.size() + "\n" +
                    "impossibleMinTemperature = " + impossibleMinTemperature.size() + "\n" +
                    "impossibleMaxTemperature = " + impossibleMaxTemperature.size() + "\n" +
                    "impossibleMinSalinity    = " + impossibleMinSalinity.size() + "\n" +
                    "impossibleMaxSalinity    = " + impossibleMaxSalinity.size() + "\n");

                //lon
                String2.log("\n*** " + impossibleNanLon.size() + 
                    " stations had invalid lon" +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleNanLon.sortIgnoreCase();
                String2.log(impossibleNanLon.toNewlineString());

                String2.log("\n*** " + impossibleMinLon.size() + 
                    " stations had lon<" + minLon +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMinLon.sortIgnoreCase();
                String2.log(impossibleMinLon.toNewlineString());

                String2.log("\n*** " + impossibleMaxLon.size() + 
                    " stations had lon>" + maxLon +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMaxLon.sortIgnoreCase();
                String2.log(impossibleMaxLon.toNewlineString());

                //lat
                String2.log("\n*** " + impossibleNanLat.size() + 
                    " stations had invalid lat" +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleNanLat.sortIgnoreCase();
                String2.log(impossibleNanLat.toNewlineString());

                String2.log("\n*** " + impossibleMinLat.size() + 
                    " stations had lat<" + minLat +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMinLat.sortIgnoreCase();
                String2.log(impossibleMinLat.toNewlineString());

                String2.log("\n*** " + impossibleMaxLat.size() + 
                    " stations had lat>" + maxLat +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMaxLat.sortIgnoreCase();
                String2.log(impossibleMaxLat.toNewlineString());

                //depth 
                String2.log("\n*** " + impossibleMinDepth.size() + 
                    " stations had depth<" + minDepth +  
                    " and good depth quality flags (" + okQFCsv + ").");
                impossibleMinDepth.sortIgnoreCase();
                String2.log(impossibleMinDepth.toNewlineString());

                String2.log("\n*** " + impossibleMaxDepth.size() + 
                    " stations had depth>" + maxDepth + 
                    " and good depth quality flags (" + okQFCsv + ").");
                impossibleMaxDepth.sortIgnoreCase();
                String2.log(impossibleMaxDepth.toNewlineString());

                //sa = impossibleLatLon.toArray();
                //Arrays.sort(sa);
                //String2.log("\n*** " + sa.length + " stations had impossible latitude or longitude values" +
                //    " and good q_pos quality flags.");
                //String2.log(String2.toNewlineString(sa));

                String2.log("\n*** " + impossibleMinTemperature.size() + 
                    " stations had temperature<" + minTemperature + 
                    " and good temperature quality flags (" + okQFCsv + ").");
                impossibleMinTemperature.sortIgnoreCase();
                String2.log(impossibleMinTemperature.toNewlineString());

                String2.log("\n*** " + impossibleMaxTemperature.size() + 
                    " stations had temperature>" + maxTemperature + 
                    " and good temperature quality flags (" + okQFCsv + ").");
                impossibleMaxTemperature.sortIgnoreCase();
                String2.log(impossibleMaxTemperature.toNewlineString());

                String2.log("\n*** " + impossibleMinSalinity.size() + 
                    " stations had salinity<" + minSalinity + 
                    " and good salinity quality flags (" + okQFCsv + ").");
                impossibleMinSalinity.sortIgnoreCase();
                String2.log(impossibleMinSalinity.toNewlineString());

                String2.log("\n*** " + impossibleMaxSalinity.size() + 
                    " stations had salinity>" + maxSalinity + 
                    " and good salinity quality flags (" + okQFCsv + ").");
                impossibleMaxSalinity.sortIgnoreCase();
                String2.log(impossibleMaxSalinity.toNewlineString());

            }

            //are we done?
            if (year == lastYear && month == lastMonth)
                break;

            //increment the month
            month++;
            if (month == 13) {
                year++; 
                month = 1;
            }

        }  //end of month/year loop

        String2.log("\n*** bobConsolidateGtspp completely finished " + 
            firstYear + "-" + firstMonth +
            " through " + lastYear + "-" + lastMonth);

        String2.log("\n***" +
            "\ntotalNExceptions=    " + totalNExceptions     + "        totalNWarnings=       " + totalNWarnings +
            "\ntotalNBadStation=    " + totalNBadStation     + "        totalNGoodStation=    " + totalNGoodStation + 
            "\ntotalNBadPos=        " + totalNBadPos         + "        totalNGoodPos=        " + totalNGoodPos + 
            "\ntotalNBadTime=       " + totalNBadTime        + "        totalNGoodTime=       " + totalNGoodTime + 
            "\ntotalNBadDepth=      " + totalNBadDepth       + "        totalNGoodDepth=      " + totalNGoodDepth + 
            "\ntotalNBadTemperature=" + totalNBadTemperature + "        totalNGoodTemperature=" + totalNGoodTemperature + 
            "\ntotalNBadSalinity=   " + totalNBadSalinity    + "        totalNGoodSalinity=   " + totalNGoodSalinity + 
            "\ntotalNBadRows=       " + totalNBadRows        + "        totalNGoodRows=       " + totalNGoodRows + 
            "\nlogFile=F:/data/gtspp/log.txt" +
            "\n\n*** all finished time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - elapsedTime));
    }

    /** Almost identical to method above, 
     * but for .zip files with the GTSPP .nc format used until the beginning of 2012 */
     //THIS WORKS ON THE OLD FORMAT GTSPP DATA.  COMMENTED OUT TO AVOID CONFUSION.
/*    public static void bobConsolidateGtsppPre2012(int firstYear, int firstMonth,
        int lastYear, int lastMonth, boolean testMode) throws Throwable {

        int chunkSize = 30;  //lon width, lat height of a tile, in degrees
        int minLat = -90;
        int minLon = -180;
        int maxLon = 180;
        String zipDir      = "f:/data/gtspp/bestNcZip/"; //gtspp_at199001.zip
        String destDir     = "f:/data/gtspp/bestNcConsolidated/";
        String tempDir     = "f:/data/gtspp/temp/"; 
        String testTempDir = "f:/data/gtspp/testTemp/"; 
        String testDestDir = "f:/data/gtspp/testDest/";
        String logFile     = "f:/data/gtspp/log.txt"; 
        File2.makeDirectory(tempDir);
        String okQF = "125";  //1=correct, 2=probably correct, 5=modified (so now correct)
        float temperatureMV = -99;
        float salinityMV = -99;
        char qMV = '9';
        String timeUnits = "days since 1900-01-01 00:00:00"; //causes roundoff error(!)
        double timeBaseAndFactor[] = Calendar2.getTimeBaseAndFactor(timeUnits);
        //impossible values:
        float minDepth       = 0,  maxDepth = 10000;
        float minTemperature = -2, maxTemperature = 40;
        float minSalinity    = 0,  maxSalinity = 41;

        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        if (testMode) {
            firstYear = 1990; firstMonth = 1;
            lastYear = 1990; lastMonth = 1;
        }

        SSR.verbose = false;
        
        String2.setupLog(true, false, 
            logFile, false, false, Integer.MAX_VALUE);

        String2.log("*** bobConsolidateGtsppZip");
        long elapsedTime = System.currentTimeMillis();
        int nLon = 360 / chunkSize;
        int nLat = 180 / chunkSize;
        //q_pos (position quality flag), q_date_time (time quality flag)
        int stationCol = -1, organizationCol = -1, dataTypeCol = -1, cruiseCol = -1,
            longitudeCol = -1, latitudeCol = -1, timeCol = -1, 
            depthCol = -1, temperatureCol = -1, salinityCol = -1;
        int totalNGoodStation = 0, totalNGoodPos = 0, totalNGoodTime = 0, 
            totalNGoodDepth = 0, totalNGoodTemperature = 0, totalNGoodSalinity = 0;
        int totalNBadStation = 0, totalNBadPos = 0, totalNBadTime = 0, 
            totalNBadDepth = 0, totalNBadTemperature = 0, totalNBadSalinity = 0,
            totalNWarnings = 0, totalNExceptions = 0;
        long totalNGoodRows = 0, totalNBadRows = 0;
        //StringArray impossibleNaNDepth = new StringArray();
        StringArray impossibleMinDepth = new StringArray();
        StringArray impossibleMaxDepth = new StringArray();
        //StringArray impossibleNanTemperature = new StringArray();
        StringArray impossibleMinTemperature = new StringArray();
        StringArray impossibleMaxTemperature = new StringArray();
        //StringArray impossibleNanSalinity = new StringArray();
        StringArray impossibleMinSalinity = new StringArray();
        StringArray impossibleMaxSalinity = new StringArray();

        //*** process a month's data
        int year = firstYear;
        int month = firstMonth;
        while (year <= lastYear) {
            //are we done?
            if (year == lastYear && month > lastMonth)
                break;

            String zMonth  = String2.zeroPad("" + month,       2);
            String zMonth1 = String2.zeroPad("" + (month + 1), 2);
            double minEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth  + "-01");
            double maxEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth1 + "-01");

            //make tables to hold a table for each tile
            Table table[][] = new Table[nLon][nLat];
            for (int loni = 0; loni < nLon; loni++) {
                for (int lati = 0; lati < nLat; lati++) {
                    Table tTable = new Table();
                    Attributes ga = tTable.globalAttributes();
                    ga.add("history", 
                        ".zip files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ (http://www.nodc.noaa.gov/GTSPP/)\n" +
                        today + " Most recent ingest, clean, and reformat at ERD (bob.simons at noaa.gov).");
                    ga.add("infoUrl",    "http://www.nodc.noaa.gov/GTSPP/");
                    ga.add("institution","NOAA NODC");
                    ga.add("title",      "Global Temperature-Salinity Profile Program (GTSPP) Data");

                    stationCol       = tTable.addColumn(tTable.nColumns(), "station_id",        new IntArray(),
                        new Attributes());
                    organizationCol  = tTable.addColumn(tTable.nColumns(), "org",               new StringArray(),
                        new Attributes());
                    dataTypeCol      = tTable.addColumn(tTable.nColumns(), "type",              new StringArray(),
                        new Attributes());
                    cruiseCol        = tTable.addColumn(tTable.nColumns(), "cruise",            new StringArray(),
                        new Attributes());
                    longitudeCol     = tTable.addColumn(tTable.nColumns(), "longitude",         new FloatArray(),
                        (new Attributes()).add("units", "degrees_east"));
                    latitudeCol      = tTable.addColumn(tTable.nColumns(), "latitude",          new FloatArray(),
                        (new Attributes()).add("units", "degrees_north"));
                    timeCol          = tTable.addColumn(tTable.nColumns(), "time",              new DoubleArray(),
                        (new Attributes()).add("units", EDV.TIME_UNITS));
                    depthCol         = tTable.addColumn(tTable.nColumns(), "depth",             new FloatArray(),
                        (new Attributes()).add("units", "m"));
                    temperatureCol   = tTable.addColumn(tTable.nColumns(), "temperature",       new FloatArray(),
                        (new Attributes()).add("units", "degree_C"));
                    salinityCol      = tTable.addColumn(tTable.nColumns(), "salinity",          new FloatArray(),
                        (new Attributes()).add("units", "PSU"));

                    table[loni][lati] = tTable;
                }
            }                         

            //destination directory
            String tDestDir = testMode? testDestDir : destDir + year + "/" + zMonth + "/";
            File2.makeDirectory(tDestDir);
            //make sure all files are deleted 
            for (int i = 0; i < 1000000; i++) {
                if (i % 10 == 0) 
                    File2.deleteAllFiles(tDestDir);
                File tf = new File(tDestDir);
                File files[] = tf.listFiles();
                if (files.length == 0)
                    break;
                Math2.gc(2000); //good time to gc
                Math2.gc(2000); //good time to gc
            }


            //unzip all atlantic, indian, and pacific .zip files for that month 
            String region2[] = {"at", "in", "pa"};
            int nRegions = testMode? 1 : 3;
            for (int region = 0; region < nRegions; region++) {
                String sourceZipJustFileName = "gtspp_" + region2[region] + year + zMonth + ".zip";
                String sourceZipName = zipDir + sourceZipJustFileName;


                if (!testMode) {

                    //delete all files in tempDir
                    int waitSeconds = 2;
                    int nAttempts = 100;
                    for (int attempt = 0; attempt < nAttempts; attempt++) {
                        if (attempt == 0 || waitSeconds > 60)
                            File2.deleteAllFiles(tempDir);
                        Math2.gc(waitSeconds * 1000); //good time to gc
                        File tempDirFile = new File(tempDir);
                        File files[] = tempDirFile.listFiles();
                        if (files.length == 0)
                            break;
                        waitSeconds *= 2;
                    }

                    //unzip file into tempDir         //gtspp_at199001.zip
                    String2.log("\n*** unzipping " + sourceZipName);
                    SSR.unzip(sourceZipName,
                        tempDir, true, 100 * 60); //ignoreZipDirectories, timeOutSeconds 100 minutes
                }

                //read each file and put data in proper table
                String tTempDir = testMode? testTempDir : tempDir;
                File tTempDirAsFile = new File(tTempDir);
                String sourceFileNames[] = tTempDirAsFile.list(); //just the file names
                String2.log("unzipped " + sourceFileNames.length + " files");
                int nSourceFileNames = //testMode? 100 : 
                    sourceFileNames.length;
                int nGoodStation = 0, nGoodPos = 0, nGoodTime = 0, 
                    nGoodDepth = 0, nGoodTemperature = 0, nGoodSalinity = 0, nGoodRows = 0;
                int nBadStation = 0, nBadPos = 0, nBadTime = 0, 
                    nBadDepth = 0, nBadTemperature = 0, nBadSalinity = 0, nBadRows = 0,
                    nWarnings = 0, nExceptions = 0;
                for (int sfi = 0; sfi < nSourceFileNames; sfi++) {
                    String sourceFileName = sourceFileNames[sfi];
                    if (sfi % 10000 == 0) {
                        if (sfi > 0) 
                            Math2.gc(3 * 1000); //good time to gc
                        //high water mark is ~160 MB, so memory not a problem
                        String2.log("file #" + sfi + " " + Math2.memoryString());
                    }

                    if (!sourceFileName.endsWith(".nc")) {
                        String2.log("ERROR: not a .nc file: " + sourceFileName);
                        continue;
                    }

                    NetcdfFile ncFile = null; 

                    try {
                        //get the station name
                        String stationName = sourceFileName.substring(0, sourceFileName.length() - 3); 
                        if (!stationName.matches("\\d+")) {//all digits
                            nBadStation++;
                            throw new SimpleException("Invalid stationName=" + stationName);
                        }
                        int station = String2.parseInt(stationName);
                        nGoodStation++;

                        //open the file
                        ncFile = NcHelper.openFile(tTempDir + sourceFileName);
                        Variable var;
                        Attributes tVarAtts = new Attributes();
                        String tUnits;

                        //get all of the data 

                        //stream_ident
                        var = ncFile.findVariable("stream_ident");                        
                        String organization = "";
                        String dataType = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No stream_ident in " + sourceFileName);
                        } else {
                            PrimitiveArray streamPA = NcHelper.getPrimitiveArray(var);
                            if (streamPA instanceof StringArray && streamPA.size() > 0) {
                                String stream = streamPA.getString(0);
                                if (stream.length() >= 4) {
                                    organization = stream.substring(0, 2).trim();
                                    dataType = stream.substring(2, 4).trim();
                                } else {
                                    String2.log("WARNING: stream_ident isn't a 4 char string: " + stream);
                                }
                            } else {
                                String2.log("WARNING: stream_ident isn't a StringArray: " + 
                                    streamPA.toString());
                            }
                        }

                        //cruise
                        var = ncFile.findVariable("cruise_id");                        
                        String cruise = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No cruise_id in " + sourceFileName);
                        } else {
                            PrimitiveArray cruisePA = NcHelper.getPrimitiveArray(var);
                            if (cruisePA instanceof StringArray && cruisePA.size() > 0) {
                                cruise = cruisePA.getString(0).trim();
                            } else {
                                String2.log("WARNING: cruise_id isn't a StringArray: " + 
                                    cruisePA.toString());
                            }
                        }
                        //String2.log("orgainization=" + organization + " data_type=" + dataType + " cruise=" + cruise);

                        //position quality flag 
                        var = ncFile.findVariable("q_pos");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No q_pos in " + sourceFileName);
                        } else {
                            PrimitiveArray q_pos = NcHelper.getPrimitiveArray(var);
                            if (!(q_pos instanceof StringArray) || q_pos.size() != 1) 
                                throw new SimpleException("Invalid q_pos=" + q_pos);
                            String ts = q_pos.getString(0);
                            if (ts.length() != 1) 
                                throw new SimpleException("Invalid q_pos ts=" + ts);
                            if (okQF.indexOf(ts.charAt(0)) < 0) {
                                //String2.log("Bad q_pos ts=" + ts); //see 1990-01 at
                                nBadPos++;
                                continue;
                            }
                            //nGoodPos++; is below
                        }

                        //time quality flag 
                        var = ncFile.findVariable("q_date_time");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No q_date_time in " + sourceFileName);
                        } else {
                            PrimitiveArray q_date_time = NcHelper.getPrimitiveArray(var);
                            if (!(q_date_time instanceof StringArray) || q_date_time.size() != 1) 
                                throw new SimpleException("Invalid q_date_time=" + q_date_time);
                            String ts = q_date_time.getString(0);
                            if (ts.length() != 1) 
                                throw new SimpleException("Invalid q_date_time ts=" + ts);
                            if (okQF.indexOf(ts.charAt(0)) < 0) {
                                //String2.log("Bad q_date_time ts=" + ts);
                                nBadTime++;
                                continue;
                            }
                            //nGoodTime is below
                        }

                        //time
                        var = ncFile.findVariable("time");                        
                        if (var == null) 
                            throw new SimpleException("No time!");
                        tVarAtts.clear();
                        NcHelper.getVariableAttributes(var, tVarAtts);
                        tUnits = tVarAtts.getString("units");
                        if (!timeUnits.equals(tUnits)) 
                            throw new SimpleException("Invalid time units=" + tUnits);
                        PrimitiveArray time = NcHelper.getPrimitiveArray(var);
                        if (!(time instanceof DoubleArray) || time.size() != 1) 
                            throw new SimpleException("Invalid time=" + time);
                        double tTime = Calendar2.unitsSinceToEpochSeconds(
                            timeBaseAndFactor[0], timeBaseAndFactor[1], time.getDouble(0));
                        String isoTime = Calendar2.safeEpochSecondsToIsoStringT(tTime, "");
                        if (tTime < minEpochSeconds || tTime > maxEpochSeconds) 
                            throw new SimpleException("Invalid tTime=" + isoTime);
                        //original times (that I looked at) are to nearest second
                        //so round to nearest second (fix .99999 problems)
                        tTime = Math.rint(tTime); 
                        nGoodTime++;

                        //longitude
                        var = ncFile.findVariable("longitude");                        
                        if (var == null) 
                            throw new SimpleException("No longitude!");
                        PrimitiveArray longitude = NcHelper.getPrimitiveArray(var);
                        if (!(longitude instanceof FloatArray) || longitude.size() != 1) 
                            throw new SimpleException("Invalid longitude=" + longitude);

                        //latitude
                        var = ncFile.findVariable("latitude");                        
                        if (var == null) 
                            throw new SimpleException("No latitude!");
                        PrimitiveArray latitude = NcHelper.getPrimitiveArray(var);
                        if (!(latitude instanceof FloatArray) || latitude.size() != 1) 
                            throw new SimpleException("Invalid latitude=" + latitude);
                        nGoodPos++;

                        //depth
                        var = ncFile.findVariable("depth");                        
                        if (var == null) 
                            throw new SimpleException("No depth!");
                        PrimitiveArray depth = NcHelper.getPrimitiveArray(var);
                        if (!(depth instanceof FloatArray) || depth.size() == 0) 
                            throw new SimpleException("Invalid depth=" + depth);
                        int nDepth = depth.size();

                        //DEPH_qparm
                        var = ncFile.findVariable("DEPH_qparm");                        
                        if (var == null) 
                            throw new SimpleException("No DEPH_qparm!");
                        PrimitiveArray DEPH_qparm = NcHelper.getPrimitiveArray(var);
                        if (!(DEPH_qparm instanceof StringArray) || DEPH_qparm.size() != nDepth) 
                            throw new SimpleException("Invalid DEPH_qparm=" + DEPH_qparm);
                        //nGoodDepth is below

                        //temperature
                        var = ncFile.findVariable("temperature");                        
                        PrimitiveArray temperature;
                        PrimitiveArray TEMP_qparm;
                        float temperatureFV = temperatureMV;
                        if (var == null) {
                            //nWarnings++;
                            //String2.log("WARNING: No temperature in " + sourceFileName); reasonably common
                            temperature = PrimitiveArray.factory(float.class,  nDepth, "" + temperatureMV);
                            TEMP_qparm  = PrimitiveArray.factory(String.class, nDepth, "" + qMV);
                        } else {            
                            temperature = NcHelper.getPrimitiveArray(var);
                            if (!(temperature instanceof FloatArray) || temperature.size() != nDepth) 
                                throw new SimpleException("Invalid temperature=" + temperature);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            temperatureFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(temperatureFV) && temperatureFV != temperatureMV)
                                throw new SimpleException("Invalid temperature _FillValue=" + temperatureFV);

                            //TEMP_qparm
                            var = ncFile.findVariable("TEMP_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No TEMP_qparm in " + sourceFileName);
                                TEMP_qparm = PrimitiveArray.factory(String.class,  nDepth, "" + qMV);
                            } else {
                                TEMP_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(TEMP_qparm instanceof StringArray) || TEMP_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid TEMP_qparm=" + TEMP_qparm);
                            }
                        }

                        //salinity
                        var = ncFile.findVariable("salinity");                        
                        PrimitiveArray salinity;
                        PrimitiveArray PSAL_qparm;
                        float salinityFV = salinityMV;
                        if (var == null) {
                            //String2.log("WARNING: No salinity in " + sourceFileName);   //very common
                            salinity   = PrimitiveArray.factory(float.class,  nDepth, "" + salinityMV);
                            PSAL_qparm = PrimitiveArray.factory(String.class, nDepth, "" + qMV);
                        } else {
                            salinity = NcHelper.getPrimitiveArray(var);
                            if (!(salinity instanceof FloatArray) || salinity.size() != nDepth) 
                                throw new SimpleException("Invalid salinity=" + salinity);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            salinityFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(salinityFV) && salinityFV != salinityMV)
                                throw new SimpleException("Invalid salinity _FillValue=" + salinityFV);

                            //PSAL_qparm
                            var = ncFile.findVariable("PSAL_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No PSAL_qparm in " + sourceFileName);
                                PSAL_qparm = PrimitiveArray.factory(String.class,  nDepth, "" + qMV);
                            } else {
                                PSAL_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(PSAL_qparm instanceof StringArray) || PSAL_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid PSAL_qparm=" + PSAL_qparm);
                            }                   
                        }

                        //clean the data
                        //(good to do it here so memory usage is low -- table remains as small as possible)
                        //Change "impossible" data to NaN
                        //(from http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf
                        //pg 61 has Table 2.1: Global Impossible Parameter Values).
                        BitSet keep = new BitSet();
                        keep.set(0, nDepth);  //all true 

                        //find worst impossible depth/temperature/salinity for this station
                        //boolean tImpossibleNanDepth       = false;
                        //boolean tImpossibleNanTemperature = false;
                        //boolean tImpossibleNanSalinity    = false;
                        float tImpossibleMinDepth = minDepth;
                        float tImpossibleMaxDepth = maxDepth;
                        float tImpossibleMinTemperature = minTemperature;
                        float tImpossibleMaxTemperature = maxTemperature;
                        float tImpossibleMinSalinity = minSalinity;
                        float tImpossibleMaxSalinity = maxSalinity;

                        for (int row = 0; row < nDepth; row++) {

                            //DEPH_qparm
                            String qs = DEPH_qparm.getString(row);
                            float f = depth.getFloat(row);
                            if (qs.length() != 1) 
                                throw new SimpleException("Invalid DEPH_qparm(" + row + ")=" + qs);
                            if (okQF.indexOf(qs.charAt(0)) < 0) {
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (Float.isNaN(f)) { //"impossible" depth
                                //tImpossibleNanDepth = true;
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f < minDepth) {
                                tImpossibleMinDepth = Math.min(tImpossibleMinDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f > maxDepth) { 
                                tImpossibleMaxDepth = Math.max(tImpossibleMaxDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            }
                            nGoodDepth++;

                            boolean hasData = false;

                            //temperature
                            qs = TEMP_qparm.getString(row);
                            if (qs.length() != 1) 
                                throw new SimpleException("Invalid TEMP_qparm(" + row + ")=" + qs);
                            f = temperature.getFloat(row);
                            if (okQF.indexOf(qs.charAt(0)) < 0) {
                                temperature.setString(row, "");  //so bad value is now NaN
                                nBadTemperature++;
                            } else if (Float.isNaN(f) || f == temperatureMV) {
                                //tImpossibleNanTemperature = true;
                                temperature.setString(row, "");  //so missing value is now NaN
                                nBadTemperature++;
                            } else if (f < minTemperature) { //"impossible" water temperature
                                tImpossibleMinTemperature = Math.min(tImpossibleMinTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else if (f > maxTemperature) { //"impossible" water temperature
                                tImpossibleMaxTemperature = Math.max(tImpossibleMaxTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else {
                                nGoodTemperature++;
                                hasData = true;
                            }

                            //salinity
                            qs = PSAL_qparm.getString(row);
                            if (qs.length() != 1) 
                                throw new SimpleException("Invalid PSAL_qparm(" + row + ")=" + qs);
                            f = salinity.getFloat(row);
                            if (okQF.indexOf(qs.charAt(0)) < 0) {
                                salinity.setString(row, "");  //so bad value is now NaN
                                nBadSalinity++;
                            } else if (Float.isNaN(f) || f == salinityMV) {
                                //tImpossibleNanSalinity = true;
                                salinity.setString(row, "");  //so missing value is now NaN
                                nBadSalinity++;
                            } else if (f < minSalinity) { //"impossible" salinity
                                tImpossibleMinSalinity = Math.min(tImpossibleMinSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else if (f > maxSalinity) { //"impossible" salinity
                                tImpossibleMaxSalinity = Math.max(tImpossibleMaxSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else {
                                nGoodSalinity++;
                                hasData = true;
                            }

                            //no valid temperature or salinity data?
                            if (!hasData) {           
                                keep.clear(row);
                            }
                        }

                        //ensure sizes still correct
                        Test.ensureEqual(depth.size(),       nDepth, "depth.size changed!");
                        Test.ensureEqual(temperature.size(), nDepth, "temperature.size changed!");
                        Test.ensureEqual(salinity.size(),    nDepth, "salinity.size changed!");

                        //actually remove the bad rows
                        int tnGood = keep.cardinality();
                        if (testMode && verbose) String2.log(sourceFileName + 
                            ": nGoodRows=" + tnGood + 
                            " nBadRows=" + (nDepth - tnGood));
                        nGoodRows += tnGood;
                        nBadRows += nDepth - tnGood;
                        depth.justKeep(keep);
                        temperature.justKeep(keep);
                        salinity.justKeep(keep);
                        nDepth = depth.size();

                        //impossible
                        String key = sourceZipJustFileName + " " + stationName;
                        //if (tImpossibleNanDepth)
                        //     impossibleNanDepth.add(key + " hasNaN=true");
                        //if (tImpossibleNanTemperature)
                        //     impossibleNanTemperature.add(key + " hasNaN=true");
                        //if (tImpossibleNanSalinity)
                        //     impossibleNanSalinity.add(key + " hasNaN=true");

                        if (tImpossibleMinDepth < minDepth)
                             impossibleMinDepth.add(key + " worst = " + tImpossibleMinDepth);
                        if (tImpossibleMaxDepth > maxDepth)
                             impossibleMaxDepth.add(key + " worst = " + tImpossibleMaxDepth);
                        if (tImpossibleMinTemperature < minTemperature)
                             impossibleMinTemperature.add(key + " worst = " + tImpossibleMinTemperature);
                        if (tImpossibleMaxTemperature > maxTemperature)
                             impossibleMaxTemperature.add(key + " worst = " + tImpossibleMaxTemperature);
                        if (tImpossibleMinSalinity < minSalinity)
                             impossibleMinSalinity.add(key + " worst = " + tImpossibleMinSalinity);
                        if (tImpossibleMaxSalinity > maxSalinity)
                             impossibleMaxSalinity.add(key + " worst = " + tImpossibleMaxSalinity);

                        //which table
                        float lon = (float)Math2.anglePM180(longitude.getFloat(0));
                        float lat = latitude.getFloat(0);
                        if (lon < minLon || lon > maxLon ||
                            lat < -90 || lat > 90) {
                            //impossibleLatLon.add(sourceZipJustFileName + " " + stationName);
                            nGoodPos--; //adjust for above
                            nBadPos++;
                            throw new SimpleException("Invalid lon=" + lon + " or lat=" + lat);
                        }
                        int loni = Math2.roundToInt(Math.floor((lon - minLon) / chunkSize));
                        int lati = Math2.roundToInt(Math.floor((lat - minLat) / chunkSize));
                        //if maxLon or maxLat (which is valid), put data in last valid table
                        if (loni == nLon) loni = nLon - 1;
                        if (lati == nLat) lati = nLat - 1;
                        Table tTable = table[loni][lati];

                        //put data in tTable
                        int oNRows = tTable.nRows();
                        ((IntArray   )tTable.getColumn(stationCol)).addN(nDepth, station);
                        ((StringArray)tTable.getColumn(organizationCol)).addN(nDepth, organization);
                        ((StringArray)tTable.getColumn(dataTypeCol)).addN(nDepth, dataType);
                        ((StringArray)tTable.getColumn(cruiseCol)).addN(nDepth, cruise);
                        ((FloatArray )tTable.getColumn(longitudeCol)).addN(nDepth, lon);
                        ((FloatArray )tTable.getColumn(latitudeCol)).addN(nDepth, lat);
                        ((DoubleArray)tTable.getColumn(timeCol)).addN(nDepth, tTime);
                        ((FloatArray )tTable.getColumn(depthCol)).append(depth);
                        ((FloatArray )tTable.getColumn(temperatureCol)).append(temperature);
                        ((FloatArray )tTable.getColumn(salinityCol)).append(salinity);

                        //ensure the table is valid (same size for each column)
                        tTable.ensureValid();

                    } catch (Throwable t) {
                        nExceptions++;
                        String2.log("ERROR while processing " + sourceFileName + "\n  " + 
                            MustBe.throwableToString(t));
                    } finally {
                        //always close the ncFile
                        if (ncFile != null) {
                            try {
                                ncFile.close(); 
                            } catch (Throwable t) {
                                String2.log("ERROR: unable to close " + sourceFileName + "\n" +
                                    MustBe.getShortErrorMessage(t));
                            }
                        }
                    }
                }
                //end of region loop
                String2.log("\nIn zip=" + sourceZipName + 
                    "\n nExceptions=    " + nExceptions     + "        nWarnings="        + nWarnings +
                    "\n nBadStation=    " + nBadStation     + "        nGoodStation="     + nGoodStation +
                    "\n nBadPos=        " + nBadPos         + "        nGoodPos="         + nGoodPos +
                    "\n nBadTime=       " + nBadTime        + "        nGoodTime="        + nGoodTime +
                    "\n nBadDepth=      " + nBadDepth       + "        nGoodDepth="       + nGoodDepth +
                    "\n nBadTemperature=" + nBadTemperature + "        nGoodTemperature=" + nGoodTemperature +
                    "\n nBadSalinity=   " + nBadSalinity    + "        nGoodSalinity="    + nGoodSalinity);
                totalNGoodStation += nGoodStation;
                totalNGoodPos += nGoodPos;
                totalNGoodTime += nGoodTime;
                totalNGoodDepth += nGoodDepth; 
                totalNGoodTemperature += nGoodTemperature; 
                totalNGoodSalinity += nGoodSalinity;
                totalNGoodRows += nGoodRows;
                totalNBadPos += nBadPos; 
                totalNBadTime += nBadTime; 
                totalNBadDepth += nBadDepth; 
                totalNBadTemperature += nBadTemperature; 
                totalNBadSalinity += nBadSalinity;
                totalNBadRows += nBadRows;
                totalNWarnings += nWarnings;
                totalNExceptions += nExceptions;
            } //region

            //save the tile tables if they have any data
            boolean filePrinted = false;
            for (int loni = 0; loni < nLon; loni++) {
                for (int lati = 0; lati < nLat; lati++) {
                    Table tTable = table[loni][lati];
                    if (tTable.nRows() == 0)
                        continue;

                    //sort by time, station, depth  
                    //depth matches the source files: from surface to deepest
                    tTable.sort(new int[]{timeCol, stationCol, depthCol}, 
                            new boolean[]{true,    true,       true});

                    //save it
                    String tName = tDestDir + 
                        year + "-" + zMonth + "_" + 
                        (minLon + loni * chunkSize) + "E_" + 
                        (minLat + lati * chunkSize) + "N.nc";

                    tTable.saveAsFlatNc(tName,
                        "row", false); //convertToFakeMissingValues  (keep mv's as NaNs)

                    //print a file
                    if (testMode && !filePrinted) {
                        filePrinted = true;
                        String2.log(NcHelper.dumpString(tName, true));
                    }
                }
            }

            //print impossible statistics
            String2.log("\nTotal number of stations with:\n" +
                "impossibleMinDepth       = " + impossibleMinDepth.size() + "\n" +
                "impossibleMaxDepth       = " + impossibleMaxDepth.size() + "\n" +
                //"impossibleLatLon      = " + impossibleLatLon.size() + "\n" +
                "impossibleMinTemperature = " + impossibleMinTemperature.size() + "\n" +
                "impossibleMaxTemperature = " + impossibleMaxTemperature.size() + "\n" +
                "impossibleMinSalinity    = " + impossibleMinSalinity.size() + "\n" +
                "impossibleMaxSalinity    = " + impossibleMaxSalinity.size() + "\n");

            //increment the month
            month++;
            if (month == 13) {
                year++; 
                month = 1;
            }

        }
        String2.log("\n*** bobConsolidateGtspp finished " + firstYear + "-" + firstMonth +
            " through " + lastYear + "-" + lastMonth);

        String2.log("\n*** " + impossibleMinDepth.size() + " stations had depth<" + minDepth +  
            " and good depth quality flags.");
        impossibleMinDepth.sortIgnoreCase();
        String2.log(impossibleMinDepth.toNewlineString());

        String2.log("\n*** " + impossibleMaxDepth.size() + " stations had depth>" + maxDepth + 
            " and good depth quality flags.");
        impossibleMaxDepth.sortIgnoreCase();
        String2.log(impossibleMaxDepth.toNewlineString());

        //sa = impossibleLatLon.toArray();
        //Arrays.sort(sa);
        //String2.log("\n*** " + sa.length + " stations had impossible latitude or longitude values" +
        //    " and good q_pos quality flags.");
        //String2.log(String2.toNewlineString(sa));

        String2.log("\n*** " + impossibleMinTemperature.size() + " stations had temperature<" + minTemperature + 
            " and good temperature quality flags.");
        impossibleMinTemperature.sortIgnoreCase();
        String2.log(impossibleMinTemperature.toNewlineString());

        String2.log("\n*** " + impossibleMaxTemperature.size() + " stations had temperature>" + maxTemperature + 
            " and good temperature quality flags.");
        impossibleMaxTemperature.sortIgnoreCase();
        String2.log(impossibleMaxTemperature.toNewlineString());

        String2.log("\n*** " + impossibleMinSalinity.size() + " stations had salinity<" + minSalinity + 
            " and good salinity quality flags.");
        impossibleMinSalinity.sortIgnoreCase();
        String2.log(impossibleMinSalinity.toNewlineString());

        String2.log("\n*** " + impossibleMaxSalinity.size() + " stations had salinity>" + maxSalinity + 
            " and good salinity quality flags.");
        impossibleMaxSalinity.sortIgnoreCase();
        String2.log(impossibleMaxSalinity.toNewlineString());

        String2.log("\n***" +
            "\ntotalNExceptions=    " + totalNExceptions     + "        totalNWarnings=       " + totalNWarnings +
            "\ntotalNBadStation=    " + totalNBadStation     + "        totalNGoodStation=    " + totalNGoodStation + 
            "\ntotalNBadPos=        " + totalNBadPos         + "        totalNGoodPos=        " + totalNGoodPos + 
            "\ntotalNBadTime=       " + totalNBadTime        + "        totalNGoodTime=       " + totalNGoodTime + 
            "\ntotalNBadDepth=      " + totalNBadDepth       + "        totalNGoodDepth=      " + totalNGoodDepth + 
            "\ntotalNBadTemperature=" + totalNBadTemperature + "        totalNGoodTemperature=" + totalNGoodTemperature + 
            "\ntotalNBadSalinity=   " + totalNBadSalinity    + "        totalNGoodSalinity=   " + totalNGoodSalinity + 
            "\ntotalNBadRows=       " + totalNBadRows        + "        totalNGoodRows=       " + totalNGoodRows + 
            "\nlogFile=F:/data/gtspp/log.txt" +
            "\n\n*** all finished time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - elapsedTime));
    }
*/
    /**
     * Test erdGtsppBest against source files (.zip to .nc to ncdump).
     */
    public static void testErdGtsppBest() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testErdGtsppBest");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("testErdGtsppBest"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);


        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "gtspp", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  platform {\n" +
"    String comment \"See the list of platform codes (sorted in various ways) at http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"GTSPP Platform Code\";\n" +
"    String references \"http://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html\";\n" +
"  }\n" +
"  cruise {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String comment \"Radio callsign + year for real time data, or NODC reference number for delayed mode data.  See\n" +
"http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n" +
"'X' indicates a missing value.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise_ID\";\n" +
"  }\n" +
"  org {\n" +
"    String comment \"From the first 2 characters of stream_ident:\n" +
"Code  Meaning\n" +
"AD  Australian Oceanographic Data Centre\n" +
"AF  Argentina Fisheries (Fisheries Research and Development National Institute (INIDEP), Mar del Plata, Argentina\n" +
"AO  Atlantic Oceanographic and Meteorological Lab\n" +
"AP  Asia-Pacific (International Pacific Research Center/ Asia-Pacific Data-Research Center)\n" +
"BI  BIO Bedford institute of Oceanography\n" +
"CF  Canadian Navy\n" +
"CS  CSIRO in Australia\n" +
"DA  Dalhousie University\n" +
"FN  FNOC in Monterey, California\n" +
"FR  Orstom, Brest\n" +
"FW  Fresh Water Institute (Winnipeg)\n" +
"GE  BSH, Germany\n" +
"IC  ICES\n" +
"II  IIP\n" +
"IK  Institut fur Meereskunde, Kiel\n" +
"IM  IML\n" +
"IO  IOS in Pat Bay, BC\n" +
"JA  Japanese Meteorologocal Agency\n" +
"JF  Japan Fisheries Agency\n" +
"ME  EDS\n" +
"MO  Moncton\n" +
"MU  Memorial University\n" +
"NA  NAFC\n" +
"NO  NODC (Washington)\n" +
"NW  US National Weather Service\n" +
"OD  Old Dominion Univ, USA\n" +
"RU  Russian Federation\n" +
"SA  St Andrews\n" +
"SI  Scripps Institute of Oceanography\n" +
"SO  Southampton Oceanographic Centre, UK\n" +
"TC  TOGA Subsurface Data Centre (France)\n" +
"TI  Tiberon lab US\n" +
"UB  University of BC\n" +
"UQ  University of Quebec at Rimouski\n" +
"VL  Far Eastern Regional Hydromet. Res. Inst. of V\n" +
"WH  Woods Hole\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Organization\";\n" +
"  }\n" +
"  type {\n" +
"    String comment \"From the 3rd and 4th characters of stream_ident:\n" +
"Code  Meaning\n" +
"AR  Animal mounted recorder\n" +
"BA  BATHY message\n" +
"BF  Undulating Oceanographic Recorder (e.g. Batfish CTD)\n" +
"BO  Bottle\n" +
"BT  general BT data\n" +
"CD  CTD down trace\n" +
"CT  CTD data, up or down\n" +
"CU  CTD up trace\n" +
"DB  Drifting buoy\n" +
"DD  Delayed mode drifting buoy data\n" +
"DM  Delayed mode version from originator\n" +
"DT  Digital BT\n" +
"IC  Ice core\n" +
"ID  Interpolated drifting buoy data\n" +
"IN  Ship intake samples\n" +
"MB  MBT\n" +
"MC  CTD and bottle data are mixed for the station\n" +
"MI  Data from a mixed set of instruments\n" +
"ML  Minilog\n" +
"OF  Real-time oxygen and fluorescence\n" +
"PF  Profiling float\n" +
"RM  Radio message\n" +
"RQ  Radio message with scientific QC\n" +
"SC  Sediment core\n" +
"SG  Thermosalinograph data\n" +
"ST  STD data\n" +
"SV  Sound velocity probe\n" +
"TE  TESAC message\n" +
"TG  Thermograph data\n" +
"TK  TRACKOB message\n" +
"TO  Towed CTD\n" +
"TR  Thermistor chain\n" +
"XB  XBT\n" +
"XC  Expendable CTD\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Data Type\";\n" +
"  }\n" +
"  station_id {\n" +
"    Int32 actual_range 1, 14004823;\n" +  //changes every month
"    String cf_role \"profile_id\";\n" +
"    String comment \"Identification number of the station (profile) in the GTSPP Continuously Managed Database\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID Number\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -180.0, 179.999;\n" +
"    String axis \"X\";\n" +
"    String C_format \"%9.4f\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    Int32 epic_code 502;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -78.579, 90.0;\n" +
"    String axis \"Y\";\n" +
"    String C_format \"%8.4f\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    Int32 epic_code 500;\n" +
"    String FORTRAN_format \"F8.4\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 6.31152e+8, 1.33578e+9;\n" + //2nd value changes
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float32 actual_range 0.0, 9910.0;\n" +
"    String axis \"Z\";\n" +
"    String C_format \"%6.2f\";\n" +
"    Float64 colorBarMaximum 5000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 epic_code 3;\n" +
"    String FORTRAN_format \"F6.2\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth of the Observations\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  temperature {\n" +
"    Float32 actual_range -3.91, 40.0;\n" +
"    String C_format \"%9.4f\";\n" +
"    String cell_methods \"time: point longitude: point latitude: point depth: point\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"time latitude longitude depth\";\n" +
"    Int32 epic_code 28;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  salinity {\n" +
"    Float32 actual_range 0.0, 41.0;\n" +
"    String C_format \"%9.4f\";\n" +
"    String cell_methods \"time: point longitude: point latitude: point depth: point\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String coordinates \"time latitude longitude depth\";\n" +
"    Int32 epic_code 41;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Practical Salinity\";\n" +
"    String salinity_scale \"psu\";\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +   //date at end of next line changes
"    String acknowledgment \"These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on 2012-05-25 from http://www.nodc.noaa.gov/GTSPP/.\";\n" +
"    String cdm_altitude_proxy \"depth\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"org, type, station_id, longitude, latitude, time\";\n" +
"    String cdm_trajectory_variables \"platform, cruise\";\n" +
"    String Conventions \"COARDS, WOCE, GTSPP, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String creator_email \"nodc.gtspp@noaa.gov\";\n" +
"    String creator_name \"US DOC; NESDIS; NATIONAL OCEANOGRAPHIC DATA CENTER - IN295\";\n" +
"    String creator_url \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String crs \"EPSG:4326\";\n" +
"    Float64 Easternmost_Easting 179.999;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    String file_source \"The GTSPP Continuously Managed Data Base\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -78.579;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.999;\n" +
"    Float64 geospatial_lon_min -180.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String gtspp_ConventionVersion \"GTSPP4.0\";\n" +
"    String gtspp_handbook_version \"GTSPP Data User's Manual 1.0\";\n" +
"    String gtspp_program \"writeGTSPPnc40.f90\";\n" +
"    String gtspp_programVersion \"1.7\";\n" +  //dates on next line and 3rd line change
"    String history \"2012-05-01T20:18:27Z  writeGTSPPnc40.f90 Version 1.7\n" +
".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ (http://www.nodc.noaa.gov/GTSPP/)\n" +
"2012-05-25 Most recent ingest, clean, and reformat at ERD (bob.simons at noaa.gov).\n" +
today + " (local files)\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/testErdGtsppBest.das\";\n" +
"    String id \"gtsppBest\";\n" +
"    String infoUrl \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String institution \"NOAA NODC\";\n" +
"    String keywords \"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"cruise, data, density, depth, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water\";\n" +
"    String keywords_vocabulary \"NODC Data Types, CF Standard Names, GCMD Science Keywords\";\n" +
"    String LEXICON \"NODC_GTSPP\";\n" +                                      //date below changes
"    String license \"These data are openly available to the public.  Please acknowledge the use of these data with:\n" +
"These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on 2012-05-25 from http://www.nodc.noaa.gov/GTSPP/.\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, WOCE, GTSPP, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String naming_authority \"gov.noaa.nodc\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String observationDimension \"row\";\n" +
"    String project \"Joint IODE/JCOMM Global Temperature-Salinity Profile Programme\";\n" +
"    String references \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -78.579;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"platform, cruise, org, type\";\n" +
"    String summary \"The Global Temperature-Salinity Profile Programme (GTSPP) develops and maintains a global ocean temperature and salinity resource with data that are both up-to-date and of the highest quality. It is a joint World Meteorological Organization (WMO) and Intergovernmental Oceanographic Commission (IOC) program.  It includes data from XBTs, CTDs, moored and drifting buoys, and PALACE floats. For information about organizations contributing data to GTSPP, see http://gosic.org/goos/GTSPP-data-flow.htm .  The U.S. National Oceanographic Data Center (NODC) maintains the GTSPP Continuously Managed Data Base and releases new 'best-copy' data once per month.\n" +
"\n" +
"WARNING: This dataset has a *lot* of data.  To avoid having your request fail because you are requesting too much data at once, you should almost always specify either:\n" +
"* a small time bounding box (at most, a few days), and/or\n" +
"* a small longitude and latitude bounding box (at most, several degrees square).\n" +
"Requesting data for a specific platform, cruise, org, type, and/or station_id may be slow, but it works.\n" +
"\n" +                                                            //month on next line changes
"*** This ERDDAP dataset has data for the entire world for all available times (currently, up to and including the April 2012 data) but is a subset of the original NODC 'best-copy' data.  It only includes data where the quality flags indicate the data is 1=CORRECT or 2=PROBABLY GOOD. It does not include some of the metadata, any of the history data, or any of the quality flag data of the original dataset. You can always get the complete, up-to-date dataset (and additional, near-real-time data) from the source: http://www.nodc.noaa.gov/GTSPP/ .  Specific differences are:\n" +
"* Profiles with a position_quality_flag or a time_quality_flag other than 1|2|5 were removed.\n" +
"* Rows with a depth (z) value less than 0 or greater than 10000 or a z_variable_quality_flag other than 1|2|5 were removed.\n" +
"* Temperature values less than -4 or greater than 40 or with a temperature_quality_flag other than 1|2|5 were set to NaN.\n" +
"* Salinity values less than 0 or greater than 41 or with a salinity_quality_flag other than 1|2|5 were set to NaN.\n" +
"* Time values were converted from \\\"days since 1900-01-01 00:00:00\\\" to \\\"seconds since 1970-01-01T00:00:00\\\".\n" +
"\n" +
"See the Quality Flag definitions on page 5 and \\\"Table 2.1: Global Impossible Parameter Values\\\" on page 61 of\n" +
"http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf .\n" +
"The Quality Flag definitions are also at\n" +
"http://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm .\";\n" +
"    String time_coverage_end \"2012-04-30T10:00:00Z\";\n" + //changes
"    String time_coverage_start \"1990-01-01T00:00:00Z\";\n" +
"    String title \"Global Temperature and Salinity Profile Programme (GTSPP) Data\";\n" +
"    Float64 Westernmost_Easting -180.0;\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        

        //*** .dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "gtspp", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String platform;\n" +
"    String cruise;\n" +
"    String org;\n" +
"    String type;\n" +
"    Int32 station_id;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Float32 depth;\n" +
"    Float32 temperature;\n" +
"    Float32 salinity;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //station_id    should succeed quickly (except for println statements here)
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&station_id=1254666", 
            EDStatic.fullTestCacheDirectory, "gtspp1254666", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,0.0,20.8,NaN\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,50.0,20.7,NaN\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,100.0,20.7,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //tests that should fail quickly
        String tests[] = {
            "&station_id<1",                           "&station_id=NaN",
            "&longitude<-180",   "&longitude>180",     "&longitude=NaN",
            "&latitude<-90",     "&latitude>90",       "&latitude=NaN",
            "&depth<0",          "&depth>10000",       "&depth=NaN",
            "&time<1990-01-01",  "&time>" + today,     "&time=NaN",
            "&temperature<-4",   "&temperature>40",    
            "&salinity<0",       "&salinity>41",       };
        for (int test = 0; test < tests.length; test++) { 
            try {
                String2.log("\n*** testing " + tests[test]);
                error = "";
                results = "";
                tName = tedd.makeNewFileForDapQuery(null, null, tests[test], 
                    EDStatic.fullTestCacheDirectory, "gtspp" + test, ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            } catch (Throwable t) {
                error = MustBe.throwableToString(t);
            }
            Test.ensureEqual(results, "", "results=\n" + results);
            Test.ensureTrue(error.indexOf(EDStatic.THERE_IS_NO_DATA) >= 0, "error=" + error);
        }

        //latitude = 77.0167    should succeed quickly (except for println statements here)
        tName = tedd.makeNewFileForDapQuery(null, null, "&latitude=77.0167", 
            EDStatic.fullTestCacheDirectory, "gtspp77", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,2.0,2.35,29.882\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,5.0,1.95,30.68\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,10.0,1.12,31.814\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,15.0,1.71,32.314\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,20.0,2.48,32.838\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,25.0,2.52,33.077\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,30.0,2.52,33.225\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,35.0,2.61,33.31\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,40.0,2.57,33.429\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,45.0,2.34,33.612\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,50.0,2.15,33.715\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,55.0,1.94,33.847\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,60.0,1.68,34.044\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,65.0,1.56,34.171\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,70.0,1.54,34.208\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,75.0,1.45,34.341\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,80.0,1.42,34.429\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,85.0,1.47,34.509\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,90.0,1.48,34.54\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,95.0,1.49,34.55\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,100.0,1.53,34.577\n" +
"67CE,92,NO,CT,517039,16.0,77.0167,1992-08-09T08:00:00Z,105.0,1.53,34.586\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,3.0,3.37,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,8.0,3.25,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,13.0,4.47,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,18.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,23.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,28.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,33.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,38.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,44.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,49.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,56.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,67.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,77.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,87.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,97.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,108.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,118.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,128.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,138.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,148.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,159.0,4.49,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,169.0,4.48,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,179.0,4.44,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,189.0,4.34,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,200.0,4.28,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,210.0,4.25,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,220.0,4.18,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,230.0,4.16,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,241.0,4.15,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,251.0,4.14,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,261.0,4.11,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,271.0,4.11,NaN\n" +
"33PF,62740   00,ME,TE,1264636,-11.75,77.0167,2000-01-22T08:31:00Z,282.0,4.07,NaN\n";

        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //time range     should succeed quickly (except for println statements here)
        long eTime = System.currentTimeMillis();
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&time>2000-01-01T02:59:59Z&time<2000-01-01T03:00:01Z", 
            EDStatic.fullTestCacheDirectory, "gtsppLL", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"33TT,21002   00,ME,BA,1254689,134.5333,37.9,2000-01-01T03:00:00Z,0.0,14.7,NaN\n" +
"33TT,21002   00,ME,BA,1254689,134.5333,37.9,2000-01-01T03:00:00Z,50.0,14.8,NaN\n" +
"33TT,21002   00,ME,BA,1254689,134.5333,37.9,2000-01-01T03:00:00Z,100.0,14.7,NaN\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,0.0,20.8,NaN\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,50.0,20.7,NaN\n" +
"33TT,21004   00,ME,BA,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,100.0,20.7,NaN\n" +
"33TT,22001   00,ME,BA,1254716,126.3,28.1667,2000-01-01T03:00:00Z,0.0,22.3,NaN\n" +
"33TT,22001   00,ME,BA,1254716,126.3,28.1667,2000-01-01T03:00:00Z,50.0,21.3,NaN\n" +
"33TT,22001   00,ME,BA,1254716,126.3,28.1667,2000-01-01T03:00:00Z,100.0,19.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        String2.log("time. elapsedTime=" + (System.currentTimeMillis() - eTime));
        //String2.getStringFromSystemIn("Press Enter to continue or ^C to stop ->");



    }

    /**  Really one time: resort the gtspp files.
     */
    public static void resortGtspp() throws Throwable {

        String sourceDir = "f:/data/gtspp/bestNcConsolidated/";

        //get the names of all of the .nc files
        String names[] = RegexFilenameFilter.recursiveFullNameList(sourceDir, ".*\\.nc", false);
        int n = 1; //names.length;
        for (int i = 0; i < n; i++) {
            if (i % 100 == 0) 
                String2.log("#" + i + " " + names[i]);

            //read
            Table table = new Table();
            table.readFlatNc(names[i], null, 0);

            //table.globalAttributes().set("history",
            //    "(From .zip files from http://www.nodc.noaa.gov/GTSPP/)\n" +
            //    "2010-06-16 Incremental ingest, clean, and reformat at ERD (bob.simons at noaa.gov).");

            //resort
            table.sort(
                new int[]{
                    table.findColumnNumber("time"), 
                    table.findColumnNumber("station_id"), 
                    table.findColumnNumber("depth")}, 
                new boolean[]{true, true, true});

            //write
            String tName = String2.replaceAll(names[i], "bestNcConsolidated/", "bestNcConsolidated2/");
            File2.makeDirectory(File2.getDirectory(tName));
            table.saveAsFlatNc(tName, "row", false);

            //compare
            if (i == 0) {
                String ncdump1 = NcHelper.dumpString(names[i], false);
                String ncdump2 = NcHelper.dumpString(tName,    false);
                Test.ensureEqual(ncdump1, ncdump2, "");

                String2.log("\n*** Old:\n" + NcHelper.dumpString(names[i], true));
                String2.log("\n*** New:\n" + NcHelper.dumpString(tName,    true));
            }
        }
    }

    /** This test making transparentPngs.
     */
    public static void testTransparentPng() throws Throwable {
        String2.log("\n*** testTransparentPng");
        //testVerboseOn();
        reallyVerbose = false;
        String dir = EDStatic.fullTestCacheDirectory;
        String name, tName, userDapQuery, results, expected, error;
        String dapQuery;

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet");
/*  */
        //markers on a map
        dapQuery = 
            "longitude,latitude,wtmp&time=2010-01-02T00:00:00Z" +
            "&longitude>=-140&longitude<=-110&latitude>=20&latitude<=50" +
            "&.draw=markers&.marker=5|5&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_markersMap",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPmarkersMap",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPmarkersMap500400",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //vector map
        dapQuery = 
            "longitude,latitude,wspu,wspv&time=2010-01-02T00:00:00Z" +
            "&longitude>=-140&longitude<=-110&latitude>=20&latitude<=50" +
            "&.draw=vectors&.color=0xff0000";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_vectors", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPvectors", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=360|150", 
            dir, eddTable.className() + "_TPvectors360150", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //lines on a graph
        dapQuery = 
            "time,wtmp" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=lines&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_lines", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPlines", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPlines500400", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //markers on a graph
        dapQuery = 
            "time,wtmp" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=markers&.marker=5|5&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_markers",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPmarkers",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPmarkers500400",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //sticks on a graph
        dapQuery = 
            "time,wspu,wspv" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=sticks&.color=0xff0000";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_sticks", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPsticks", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPsticks500400", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
/* */
    }


    /** 
     * This test the speed of all types of responses.
     * This test is in this class because the source data is in a file, 
     * so it has reliable access speed.
     * This gets a pretty big chunk of data.
     *
     * @param whichTest -1 for all, or 0..
     */
    public static void testSpeed(int whichTest) throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testSpeed\n" + 
            SgtUtil.isBufferedImageAccelerated() + "\n");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String tName;
        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
        //userDapQuery will make time series graphs
        //not just a 121000 points at the same location on a map
        String userDapQuery = "time,wtmp,station,longitude,latitude,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,dewp,vis,ptdy,tide,wspu,wspv&station=\"41006\""; 
        String dir = EDStatic.fullTestCacheDirectory;
        String extensions[] = new String[] {  //.help not available at this level            
            ".asc", ".csv", ".csvp", ".das", ".dds", 
            ".dods", ".esriCsv", ".geoJson", ".graph", ".html", 
            ".htmlTable", ".json", ".mat", ".nc", ".ncHeader", 
            ".odvTxt", ".subset", ".tsv", ".tsvp", ".xhtml",
            ".kml", ".smallPdf", ".pdf", ".largePdf", 
            ".smallPng", ".png", ".largePng"};  
        int expectedMs[] = new int[] { 
            //now Java 1.6                 //was Java 1.5
            //graphics tests changed a little, so not perfectly comparable
            3125, 2625, 2687, 16, 31,      //4469, 4125, 4094, 16, 32, 
            687, 3531, 5219, 47, 31,       //1782, 5156, 6922, 125, 100, 
            4400, 2719, 1531, 1922, 1797,  //6109, 4109, 4921, 4921, 4610, 
            4266, 32, 2562, 2531, 4266,    //8359, 31, 3969, 3921, 6531, 
            2078, 2500, 2063, 2047,        //4500, 5800, 5812, 5610, 
            2984, 3125, 3391};             //5421, 5204, 5343};         
        int bytes[]    = new int[] {
            18989646, 13058166, 13058203, 14544, 394, 
            11703093, 16391423, 55007762, 129296, 156701, 
            48534273, 17827554, 10485696, 9887104, 14886, 
            10698295, 21466, 13058166, 13058203, 59642150, 
            4211, 82780, 135305, 161613, 
            7482, 11367, 23684};

        //warm up
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddTable.className() + "_testSpeedw", ".pdf"); 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddTable.className() + "_testSpeedw", ".png"); 
        
        int firstExt = whichTest < 0? 0 : whichTest;
        int lastExt  = whichTest < 0? extensions.length - 1 : whichTest;
        for (int ext = firstExt; ext <= lastExt; ext++) {
            try {
                String2.log("\n*** EDDTableFromNcFiles.testSpeed test#" + ext + ": " + 
                    extensions[ext] + " speed\n");
                long time = System.currentTimeMillis();
                tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
                    dir, eddTable.className() + "_testSpeed" + ext, extensions[ext]); 
                time = System.currentTimeMillis() - time;
                long cLength = File2.length(dir + tName);
                String2.log("\n*** EDDTableFromNcFiles.testSpeed test#" + ext + ": " + 
                    extensions[ext] + " done.  " + 
                    cLength + " bytes (expected=" + bytes[ext] + ").  time=" + 
                    time + " ms (expected=" + expectedMs[ext] + ")\n"); Math2.sleep(3000);

                //display?
                if (false && String2.indexOf(imageFileTypeNames, extensions[ext]) >= 0) {
                    SSR.displayInBrowser("file://" + dir + tName);
                    Math2.gc(10000);
                }

                //size test
                Test.ensureTrue(cLength > 0.9 * bytes[ext], 
                    "File shorter than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + dir + tName);
                Test.ensureTrue(cLength < 1.1 * bytes[ext], 
                    "File longer than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + dir + tName);

                //time test
                if (time > 1.5 * Math.max(50, expectedMs[ext]))
                    throw new SimpleException(
                        "Slower than expected. observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
                if (Math.max(50, time) < 0.5 * expectedMs[ext])
                    throw new SimpleException(
                        "Faster than expected! observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");

                //data test for .nc (especially string column)
                //This is important test of EDDTable.saveAsFlatNc for nRows > partialRequestMaxCells
                //(especially since changes on 2010-09-07).
                if (extensions[ext].equals(".nc")) {
                    Table table = new Table();
                    table.readFlatNc(dir + tName, null, 1);
                    int nRows = table.nRows();
                    String2.log(".nc fileName=" + dir + tName + "\n" +
                        "nRows=" + nRows);
                    String results = table.dataToCSVString(2);
                    String expected = 
"row,time,wtmp,station,longitude,latitude,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,dewp,vis,ptdy,tide,wspu,wspv\n" +
"0,3.912804E8,24.8,41006,-77.4,29.3,297,4.1,5.3,1.0,8.3,4.8,,1014.7,22.0,,,,,3.7,-1.9\n" +
"1,3.91284E8,24.7,41006,-77.4,29.3,,,,1.1,9.1,4.5,,1014.6,22.2,,,,,,\n";
                    String2.log("results=\n" + results);
                    Test.ensureEqual(results, expected, "");
                    for (int row = 0; row < nRows; row++) {
                        Test.ensureEqual(table.getStringData(2, row), "41006", "");
                        Test.ensureEqual(table.getFloatData(3, row), -77.4f, "");
                        Test.ensureEqual(table.getFloatData(4, row), 29.3f, "");
                    }
                }
            } catch (Exception e) {
                String2.getStringFromSystemIn(
                    MustBe.throwableToString(e) +
                    "\nUnexpected ERROR for Test#" + ext + ": " + extensions[ext]+ 
                    ".  Press ^C to stop or Enter to continue..."); 
            }
        }
        reallyVerbose = oReallyVerbose;
    }


    /** 
     * This a graph with many years.
     *
     * @param whichTest -1 for all, or 0..
     */
    public static void testManyYears() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testManyYears\n");
        EDDTable eddTable = (EDDTable)oneFromDatasetXml("erdCAMarCatSY"); 
        String dir = EDStatic.fullTestCacheDirectory;
        String dapQuery = 
        "time,landings&port=%22Santa%20Barbara%22&fish=%22Abalone%22&.draw=lines&.color=0x000000";
        String tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_manyYears",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
    }


    private static void metadataToData(Table table, String colName,
        String attName, String newColName, Class tClass) throws Exception {

        int col = table.findColumnNumber(colName);
        if (col < 0)
            throw new RuntimeException("col=" + colName + " not found in " + 
                table.getColumnNamesCSSVString());
        String value = table.columnAttributes(col).getString(attName);
        table.columnAttributes(col).remove(attName);
        if (value == null)
            value = "";
        table.addColumn(table.nColumns(), newColName, 
            PrimitiveArray.factory(tClass, table.nRows(), value), 
            new Attributes());
    }

    /** NOT FOR GENERAL USE. Bob uses this to consolidate the individual WOD
     * data files into 45� x 45� x 1 month files (tiles).
     * 45� x 45� leads to 8x4=32 files for a given time point, so a request
     * for a short time but entire world opens ~32 files.
     * There are ~240 months worth of data, so a request for a small lon lat 
     * range for all time opens ~240 files.
     *
     * <p>Why tile? Because there are ~10^6 profiles/year now, so ~10^7 total.
     * And if 100 bytes of info per file for EDDTableFromFiles fileTable, that's 1 GB!.
     * So there needs to be fewer files.
     * We want to balance number of files for 1 time point (all region tiles), 
     * and number of time point files (I'll stick with their use of 1 month).
     * The tiling size selected is ok, but searches for single profile (by name)
     * are slow since a given file may have a wide range of station_ids.
     *
     * @param type the 3 letter WOD file type e.g., APB
     * @param previousDownloadDate iso date after previous download finished (e.g., 2011-05-15)
     */
/*    public static void bobConsolidateWOD(String type, String previousDownloadDate) 
        throws Throwable {

        //constants
        int chunkSize = 45;  //lon width, lat height of a tile, in degrees
        String sourceDir   = "f:/data/wod/monthly/" + type + "/"; 
        String destDir     = "f:/data/wod/consolidated/" + type + "/";
        String logFile     = "f:/data/wod/bobConsolidateWOD.log"; 

        //derived
        double previousDownload = Calendar2.isoStringToEpochSeconds(previousDownloadDate);
        int nLon = 360 / chunkSize;
        int nLat = 180 / chunkSize;
        Table.verbose = false;
        Table.reallyVerbose = false;
        NcHelper.verbose = false;
        String2.setupLog(true, false, logFile, false, false, Integer.MAX_VALUE);
        String2.log("*** bobConsolidateWOD(" + type + ", " + previousDownloadDate + ")");

        //go through the source dirs
        String sourceMonthDirs[] = RegexFilenameFilter.list(sourceDir, ".*");
        for (int sd = 0; sd < sourceMonthDirs.length; sd++) {
            String2.log("\n*** Look for new files in " + sourceDir + sourceMonthDirs[sd]);
            String sourceFiles[] = RegexFilenameFilter.list(
                sourceDir + sourceMonthDirs[sd], ".*\\.nc");
            int nSourceFiles = 
100; //sourceFiles.length;

            //are any files newer than lastDownload?
            boolean newer = false;
            for (int sf = 0; sf < nSourceFiles; sf++) {
                if (File2.getLastModified(sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf]) >
                    previousDownload) {
                    newer = true;
                    break;
                }
            }
            if (!newer) 
                continue;

            //make/empty the destDirectory for this sourceMonths
            File2.makeDirectory( destDir + sourceMonthDirs[sd]);
            File2.deleteAllFiles(destDir + sourceMonthDirs[sd]);

            //read source files
            Table cumTable = new Table();
            for (int sf = 0; sf < nSourceFiles; sf++) {
                String tFileName = sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf];
                try {
                    if (sf % 100 == 0) 
                        String2.log("reading file #" + sf + " of " + nSourceFiles);
                    Table tTable = new Table();
                    tTable.readFlat0Nc(tFileName, null, 0, -1);  //0=don't unpack  -1=read all rows
                    int tNRows = tTable.nRows();

                    //ensure expected columns
                    if (type.equals("APB")) {
                        tTable.justKeepColumns(
                                String.class, //id
                                  int.class,
                                  float.class,
                                  float.class,
                                  double.class,
                                int.class, //date
                                  float.class, 
                                  int.class,
                                  char.class,
                                  float.class,
                                char.class, //dataset
                                  float.class,
                                  float.class, //Temp
                                  int.class,
                                  int.class,
                                float.class, //Sal
                                  int.class,
                                  int.class,
                                  float.class, //Press
                                  int.class,
                                int.class,
                                  int.class, //crs
                                  int.class, 
                                  int.class, 
                                  int.class, 
                                int.class},  //WODfd
                              new String[]{
                                "WOD_cruise_identifier", 
                                  "wod_unique_cast",  
                                  "lat", 
                                  "lon", 
                                  "time", 
                                "date", 
                                  "GMT_time", 
                                  "Access_no", 
                                  "Institute", 
                                  "Orig_Stat_Num", 
                                "dataset", 
                                  "z", 
                                  "Temperature", 
                                  "Temperature_sigfigs", 
                                  "Temperature_WODflag", 
                                "Salinity", 
                                  "Salinity_sigfigs", 
                                  "Salinity_WODflag", 
                                  "Pressure",
                                "Pressure_sigfigs",
                                  "Pressure_WODflag",
                                  "crs",
                                  "profile",
                                  "WODf",
                                  "WODfp",
                                "WODfd"},
                            new Class[]{
                            });



                        if (!tTable.getColumnName(0).equals("WOD_cruise_identifier")) {
                            tTable.addColumn(0, "WOD_cruise_identifier", 
                                PrimitiveArray.factory(String.class, tNRows, ""),
                                new Attributes());
                        }
                        if (!tTable.getColumnName(6).equals("GMT_time")) {
                            tTable.addColumn(0, "GMT_time", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                        }

float GMT_time ;
        GMT_time:long_name = "GMT_time" ;
int Access_no ;
        Access_no:long_name = "NODC_accession_number" ;
        Access_no:units = "NODC_code" ;
        Access_no:comment = "used to find original data at NODC" ;
char Platform(strnlen) ;
        Platform:long_name = "Platform_name" ;
        Platform:comment = "name of platform from which measurements were taken" ;
float Orig_Stat_Num ;
        Orig_Stat_Num:long_name = "Originators_Station_Number" ;
        Orig_Stat_Num:comment = "number assigned to a given station by data originator" ;
char Cast_Direction(strnlen) ;
        Cast_Direction:long_name = "Cast_Direction" ;
char dataset(strnlen) ;
        dataset:long_name = "WOD_dataset" ;
char Recorder(strnlen) ;
        Recorder:long_name = "Recorder" ;
        Recorder:units = "WMO code 4770" ;
        Recorder:comment = "Device which recorded measurements" ;
char real_time(strnlen) ;
        real_time:long_name = "real_time_data" ;
        real_time:comment = "set if data are from the global telecommuncations system" ;
char dbase_orig(strnlen) ;
        dbase_orig:long_name = "database_origin" ;
        dbase_orig:comment = "Database from which data were extracted" ;
float z(z) ;

                        if (!tTable.getColumnName(18).equals("Salinity")) {
                            tTable.addColumn(18, "Salinity", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(19, "Salinity_sigfigs", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(20, "Salinity_WODflag", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                (new Attributes()));
                        }
                        if (!tTable.getColumnName(21).equals("Pressure")) {
                            tTable.addColumn(21, "Pressure", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(22, "Pressure_sigfigs", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(23, "Pressure_WODflag", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                (new Attributes()));
                        }
                    }


                    //convert metadata to data
                    if (type.equals("APB")) {

                        //WOD_cruise_identifier
                        metadataToData(tTable, "WOD_cruise_identifier", "country",
                            "WOD_cruise_country", String.class);
                        metadataToData(tTable, "WOD_cruise_identifier", "originators_cruise_identifier",
                            "WOD_cruise_originatorsID", String.class);
                        metadataToData(tTable, "WOD_cruise_identifier", "Primary_Investigator",
                            "WOD_cruise_Primary_Investigator", String.class);

                        //Temperature
                        metadataToData(tTable, "Temperature", "Instrument_(WOD_code)",
                            "Temperature_Instrument", String.class);
                        metadataToData(tTable, "Temperature", "WODprofile_flag",
                            "Temperature_WODprofile_flag", int.class);
                    }

                    //validate
                    double stats[];
                    int tNCols = tTable.nColumns();
                    PrimitiveArray col;

                    col = tTable.getColumn("lon");
                    stats = col.calculateStats();
                    if (stats[PrimitiveArray.STATS_MIN] < -180) 
                        String2.log("  ! minLon=" + stats[PrimitiveArray.STATS_MIN]);
                    if (stats[PrimitiveArray.STATS_MAX] > 180) 
                        String2.log("  ! maxLon=" + stats[PrimitiveArray.STATS_MAX]);

                    col = tTable.getColumn("lat");
                    stats = col.calculateStats();
                    if (stats[PrimitiveArray.STATS_MIN] < -90) 
                        String2.log("  ! minLat=" + stats[PrimitiveArray.STATS_MIN]);
                    if (stats[PrimitiveArray.STATS_MAX] > 90) 
                        String2.log("  ! maxLat=" + stats[PrimitiveArray.STATS_MAX]);

                    //append
                    if (sf == 0) {
                        cumTable = tTable;
                    } else {
                        //ensure colNames same
                        Test.ensureEqual(
                            tTable.getColumnNamesCSSVString(),
                            cumTable.getColumnNamesCSSVString(),
                            "Different column names.");

                        //append
                        cumTable.append(tTable);
                    }

                } catch (Throwable t) {
                    String2.log("ERROR: when processing " + tFileName + "\n" + 
                        MustBe.throwableToString(t));
                }
            }

            //sort
            String2.log("sorting");
            int timeCol = cumTable.findColumnNumber("time");
            int idCol   = cumTable.findColumnNumber("wod_unique_cast");
            Test.ensureNotEqual(timeCol, -1, "time column not found in " + 
                cumTable.getColumnNamesCSSVString());
            Test.ensureNotEqual(idCol, -1, "wod_unique_cast column not found in " + 
                cumTable.getColumnNamesCSSVString());
            cumTable.ascendingSort(new int[]{timeCol, idCol});

            //write consolidated data as tiles
            int cumNRows = cumTable.nRows();
            PrimitiveArray lonCol = cumTable.getColumn("lon");
            PrimitiveArray latCol = cumTable.getColumn("lat");
            for (int loni = 0; loni < nLon; loni++) {
                double minLon = -180 + loni * chunkSize;
                double maxLon = minLon + chunkSize + (loni == nLon-1? 0.1 : 0);
                for (int lati = 0; lati < nLat; lati++) {
                    double minLat = -90 + lati * chunkSize;
                    double maxLat = minLat + chunkSize + (lati == nLat-1? 0.1 : 0);
                    Table tTable = (Table)cumTable.clone();
                    BitSet keep = new BitSet(cumNRows);                    
                    for (int row = 0; row < cumNRows; row++) {
                        double lon = lonCol.getDouble(row);
                        double lat = latCol.getDouble(row);
                        keep.set(row, 
                            lon >= minLon && lon < maxLon &&
                            lat >= minLat && lat < maxLat);
                    }
                    tTable.justKeep(keep);
                    if (tTable.nRows() == 0) {
                        String2.log("No data for minLon=" + minLon + " minLat=" + minLat);
                    } else {
                        tTable.saveAsFlatNc(
                            destDir + sourceMonthDirs[sd] + "/" +
                                sourceMonthDirs[sd] + "_" + 
                                Math.round(minLon) + "E_" +
                                Math.round(minLat) + "N",
                            "row", false); //convertToFakeMissingValues
                    }
                }
            }
        }
    }
*/

    /** For WOD, get all source variable names and file they are in.
     */
    public static void getAllSourceVariableNames(String dir, String fileNameRegex) {
        HashSet hashset = new HashSet();
        String2.log("\n*** getAllsourceVariableNames from " + dir + " " + fileNameRegex);
        Table.verbose = false;
        Table.reallyVerbose = false;
        String sourceFiles[] = RegexFilenameFilter.recursiveFullNameList(
            dir, fileNameRegex, false);
        int nSourceFiles = sourceFiles.length;

        Table table = new Table();
        for (int sf = 0; sf < nSourceFiles; sf++) {
            try {
                table.readNDNc(sourceFiles[sf], null, null, 0, 0, true); // getMetadata
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
            int nCols = table.nColumns();
            for (int c = 0; c < nCols; c++) {
                String colName = table.getColumnName(c);
                if (hashset.add(colName)) {
                    String2.log("colName=" + colName + 
                          "  " + table.getColumn(c).elementClassString() +
                        "\n  file=" + sourceFiles[sf] + 

                        "\n  attributes=\n" + table.columnAttributes(c).toString());
                }
            }
        }
    }

    /*  rare in APB:
colName=Salinity
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    coordinates="time lat lon z"
    flag_definitions="WODfp"
    grid_mapping="crs"
    long_name="Salinity"
    standard_name="sea_water_salinity"
    WODprofile_flag=6

colName=Salinity_sigfigs
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=

colName=Salinity_WODflag
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    flag_definitions="WODf"

colName=Pressure
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    coordinates="time lat lon z"
    grid_mapping="crs"
    long_name="Pressure"
    standard_name="sea_water_pressure"
    units="dbar"

colName=Pressure_sigfigs
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=

colName=Pressure_WODflag
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    flag_definitions="WODf"

colName=Orig_Stat_Num
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    comment="number assigned to a given station by data originator"
    long_name="Originators_Station_Number"

colName=Bottom_Depth
  file=f:/data/wod/monthly/APB/200904-200906/wod_012999458O.nc
  attributes=
    long_name="Bottom_Depth"
    units="meters"
*/

    /** Tests the data created by getCAMarCatShort() and getCAMarCatLong()
     * and served by erdCAMarCatSM, SY, LM, LY. */
    public static void testCAMarCat() throws Throwable {
        EDDTable eddTable;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;

        //***test short name list
        //http://las.pfeg.noaa.gov:8082/thredds/dodsC/CA_market_catch/ca_fish_grouped_short.nc.ascii
        //  ?landings[12:23][1=Los Angeles][1=Barracuda, California]
/*
landings.landings[12][1][1]
[0][0], 15356
[1][0], 93891
[2][0], 178492
[3][0], 367186
[4][0], 918303
[5][0], 454981
[6][0], 342464
[7][0], 261587
[8][0], 175979
[9][0], 113828
[10][0], 46916
[11][0], 73743

landings.time_series[12]
254568.0, 255312.0, 255984.0, 256728.0, 257448.0, 258192.0, 258912.0, 259656.0, 260400.0, 261120.0, 261864.0, 262584.0

254568 hours since 1900-01-01 = 1929-01-16T00:00:00Z
262584 hours since 1900-01-01 = 1929-12-16T00:00:00Z
*/

        for (int i = 0; i < 2; i++) {
            char SL = i==0? 'S' : 'L';

            //*** monthly
            String2.log("\n**** test erdCAMarCat" + SL + "M");
            eddTable = (EDDTable)oneFromDatasetXml("erdCAMarCat" + SL + "M"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"Los Angeles\"&fish=\"Barracuda, California\"&year=1929", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1929-01-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,15356\n" +
"1929-02-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,93891\n" +
"1929-03-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,178492\n" +
"1929-04-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,367186\n" +
"1929-05-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,918303\n" +
"1929-06-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,454981\n" +
"1929-07-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,342464\n" +
"1929-08-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,261587\n" +
"1929-09-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,175979\n" +
"1929-10-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,113828\n" +
"1929-11-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,46916\n" +
"1929-12-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,73743\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            //salmon became separable in 1972. Separation appears in Long list of names, not Short.
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&year>=1976&year<=1977", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = SL == 'S'?
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1976-01-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-02-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-03-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-04-16T00:00:00Z,1976,Salmon,All,465896\n" +
"1976-05-16T00:00:00Z,1976,Salmon,All,1932912\n" +
"1976-06-16T00:00:00Z,1976,Salmon,All,2770715\n" +
"1976-07-16T00:00:00Z,1976,Salmon,All,1780115\n" +
"1976-08-16T00:00:00Z,1976,Salmon,All,581702\n" +
"1976-09-16T00:00:00Z,1976,Salmon,All,244695\n" +
"1976-10-16T00:00:00Z,1976,Salmon,All,485\n" +
"1976-11-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-12-16T00:00:00Z,1976,Salmon,All,0\n" +
"1977-01-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-02-16T00:00:00Z,1977,Salmon,All,43\n" +
"1977-03-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-04-16T00:00:00Z,1977,Salmon,All,680999\n" +
"1977-05-16T00:00:00Z,1977,Salmon,All,1490628\n" +
"1977-06-16T00:00:00Z,1977,Salmon,All,980842\n" +
"1977-07-16T00:00:00Z,1977,Salmon,All,1533176\n" +
"1977-08-16T00:00:00Z,1977,Salmon,All,928234\n" +
"1977-09-16T00:00:00Z,1977,Salmon,All,314464\n" +
"1977-10-16T00:00:00Z,1977,Salmon,All,247\n" +
"1977-11-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-12-16T00:00:00Z,1977,Salmon,All,0\n"
:
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1976-01-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-02-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-03-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-04-16T00:00:00Z,1976,Salmon,All,465896\n" +
"1976-05-16T00:00:00Z,1976,Salmon,All,1932912\n" +
"1976-06-16T00:00:00Z,1976,Salmon,All,2770715\n" +
"1976-07-16T00:00:00Z,1976,Salmon,All,1780115\n" +
"1976-08-16T00:00:00Z,1976,Salmon,All,581702\n" +
"1976-09-16T00:00:00Z,1976,Salmon,All,244695\n" +
"1976-10-16T00:00:00Z,1976,Salmon,All,485\n" +
"1976-11-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-12-16T00:00:00Z,1976,Salmon,All,0\n" +
"1977-01-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-02-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-03-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-04-16T00:00:00Z,1977,Salmon,All,1223\n" +
"1977-05-16T00:00:00Z,1977,Salmon,All,1673\n" +
"1977-06-16T00:00:00Z,1977,Salmon,All,2333\n" +
"1977-07-16T00:00:00Z,1977,Salmon,All,1813\n" +
"1977-08-16T00:00:00Z,1977,Salmon,All,1300\n" +
"1977-09-16T00:00:00Z,1977,Salmon,All,924\n" +
"1977-10-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-11-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-12-16T00:00:00Z,1977,Salmon,All,0\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&time>=1976-01-01&time<=1977-12-31", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);


            //*** yearly 
            String2.log("\n**** test erdCAMarCat" + SL + "Y");
            eddTable = (EDDTable)oneFromDatasetXml("erdCAMarCat" + SL + "Y"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"Los Angeles\"&fish=\"Barracuda, California\"&year=1929", 
                dir, eddTable.className() + "_" + SL + "Y", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1929-07-01T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,3042726\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "Y results=\n" + results);

            //salmon became separable in 1972. Separation appears in Long list of names, not Short.
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&year>=1971&year<=1980", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = SL == 'S'?
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1971-07-01T00:00:00Z,1971,Salmon,All,8115432\n" +
"1972-07-01T00:00:00Z,1972,Salmon,All,6422171\n" +
"1973-07-01T00:00:00Z,1973,Salmon,All,9668966\n" +
"1974-07-01T00:00:00Z,1974,Salmon,All,8749013\n" +
"1975-07-01T00:00:00Z,1975,Salmon,All,6925082\n" +
"1976-07-01T00:00:00Z,1976,Salmon,All,7776520\n" +
"1977-07-01T00:00:00Z,1977,Salmon,All,5928633\n" +
"1978-07-01T00:00:00Z,1978,Salmon,All,6810880\n" +
"1979-07-01T00:00:00Z,1979,Salmon,All,8747677\n" +
"1980-07-01T00:00:00Z,1980,Salmon,All,6021953\n"
:
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1971-07-01T00:00:00Z,1971,Salmon,All,8115432\n" +
"1972-07-01T00:00:00Z,1972,Salmon,All,6422171\n" +
"1973-07-01T00:00:00Z,1973,Salmon,All,9668966\n" +
"1974-07-01T00:00:00Z,1974,Salmon,All,8749013\n" +
"1975-07-01T00:00:00Z,1975,Salmon,All,6925082\n" +
"1976-07-01T00:00:00Z,1976,Salmon,All,7776520\n" +
"1977-07-01T00:00:00Z,1977,Salmon,All,9266\n" +
"1978-07-01T00:00:00Z,1978,Salmon,All,21571\n" +
"1979-07-01T00:00:00Z,1979,Salmon,All,30020\n" +
"1980-07-01T00:00:00Z,1980,Salmon,All,40653\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&time>=1971-01-01&time<=1980-12-31", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

        
        }
        String2.log("\n**** test erdCAMarCat finished successfully");
    }


    /** Tests the data created by getCAMarCatLong()
     * and served by erdCAMarCatLM and erdCAMarCatLY. */
    public static void testCAMarCatL() throws Throwable {
        EDDTable eddTable;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;

        //*** test long name list
        //http://las.pfeg.noaa.gov:8082/thredds/dodsC/CA_market_catch/ca_fish_grouped.nc.ascii
        //  ?landings[12:23][1=Los Angeles][2=Barracuda, California]
/*
landings.landings[12][1][1]
[0][0], 15356
[1][0], 93891
[2][0], 178492
[3][0], 367186
[4][0], 918303
[5][0], 454981
[6][0], 342464
[7][0], 261587
[8][0], 175979
[9][0], 113828
[10][0], 46916
[11][0], 73743
*/

    }

    /**
     * Test making an .ncCF Point file.
     */
    public static void testNcCFPoint() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFPoint");
        //this dataset is not fromNcFiles, but test here with other testNcCF tests
        EDDTable tedd = (EDDTable)oneFromDatasetXml("nwioosCoral"); 
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "longitude,latitude,altitude,time,taxa_scientific,institution,species_code" +
            "&taxa_scientific=\"Alyconaria unident.\"", 
            EDStatic.fullTestCacheDirectory, "ncCF", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF.nc {\n" +
" dimensions:\n" +
"   row = 4;\n" +
"   taxa_scientificStringLength = 19;\n" +
"   institutionStringLength = 4;\n" +
" variables:\n" +
"   double longitude(row=4);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -125.00184631347656, -121.3140640258789; // double\n" +
"     :axis = \"X\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   double latitude(row=4);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 34.911373138427734, 47.237003326416016; // double\n" +
"     :axis = \"Y\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   double altitude(row=4);\n" +
"     :_CoordinateAxisType = \"Height\";\n" +
"     :_CoordinateZisPositive = \"up\";\n" +
"     :actual_range = 77.0, 474.0; // double\n" +
"     :axis = \"Z\";\n" +
"     :colorBarMaximum = 0.0; // double\n" +
"     :colorBarMinimum = -1500.0; // double\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Altitude\";\n" +
"     :positive = \"up\";\n" +
"     :standard_name = \"altitude\";\n" +
"     :units = \"m\";\n" +
"   double time(row=4);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 8.836128E8, 9.783072E8; // double\n" +
"     :axis = \"T\";\n" +
"     :Description = \"Year of Survey.\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Time (Beginning of Survey Year)\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   char taxa_scientific(row=4, taxa_scientificStringLength=19);\n" +
"     :coordinates = \"time latitude longitude altitude\";\n" +
"     :Description = \"Scientific name of taxa\";\n" +
"     :ioos_category = \"Taxonomy\";\n" +
"     :long_name = \"Taxa Scientific\";\n" +
"   char institution(row=4, institutionStringLength=4);\n" +
"     :coordinates = \"time latitude longitude altitude\";\n" +
"     :Description = \"Institution is either: Northwest Fisheries Science Center (FRAM Division) or Alaska Fisheries Science Center (RACE Division)\";\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Institution\";\n" +
"   double species_code(row=4);\n" +
"     :actual_range = 41101.0, 41101.0; // double\n" +
"     :coordinates = \"time latitude longitude altitude\";\n" +
"     :Description = \"Unique identifier for species.\";\n" +
"     :ioos_category = \"Taxonomy\";\n" +
"     :long_name = \"Species Code\";\n" +
"\n" +
" :cdm_data_type = \"Point\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Easternmost_Easting = -121.3140640258789; // double\n" +
" :featureType = \"Point\";\n" +
" :geospatial_lat_max = 47.237003326416016; // double\n" +
" :geospatial_lat_min = 34.911373138427734; // double\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = -121.3140640258789; // double\n" +
" :geospatial_lon_min = -125.00184631347656; // double\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_max = 474.0; // double\n" +
" :geospatial_vertical_min = 77.0; // double\n" +
" :geospatial_vertical_positive = \"up\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"" + today + " http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/nwioosCoral.ncCF?longitude,latitude,altitude,time,taxa_scientific,institution,species_code&taxa_scientific=\\\"Alyconaria unident.\\\"\";\n" +
" :id = \"ncCF\";\n" +
" :infoUrl = \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.das.info\";\n" +
" :institution = \"NOAA NWFSC\";\n" +
" :keywords = \"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"Biological Classification > Animals/Invertebrates > Cnidarians > Anthozoans/Hexacorals > Hard Or Stony Corals,\n" +
"1980-2005, abbreviation, altitude, atmosphere, beginning, coast, code, collected, coral, data, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Northernmost_Northing = 47.237003326416016; // double\n" +
" :observationDimension = \"row\";\n" +
" :sourceUrl = \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\";\n" +
" :Southernmost_Northing = 34.911373138427734; // double\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"longitude, latitude, altitude, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus\";\n" +
" :summary = \"This data contains the locations of some observations of\n" +
"cold-water/deep-sea corals off the west coast of the United States.\n" +
"Records of coral catch originate from bottom trawl surveys conducted\n" +
"from 1980 to 2001 by the Alaska Fisheries Science Center (AFSC) and\n" +
"2001 to 2005 by the Northwest Fisheries Science Center (NWFSC).\n" +
"Locational information represent the vessel mid positions (for AFSC\n" +
"survey trawls) or \\\"best position\\\" (i.e., priority order: 1) gear\n" +
"midpoint 2) vessel midpoint, 3) vessel start point, 4) vessel end\n" +
"point, 5) station coordinates for NWFSC survey trawls) conducted as\n" +
"part of regular surveys of groundfish off the coasts of Washington,\n" +
"Oregon and California by NOAA Fisheries. Only records where corals\n" +
"were identified in the total catch are included. Each catch sample\n" +
"of coral was identified down to the most specific taxonomic level\n" +
"possible by the biologists onboard, therefore identification was\n" +
"dependent on their expertise. When positive identification was not\n" +
"possible, samples were sometimes archived for future identification\n" +
"by systematist experts. Data were compiled by the NWFSC, Fishery\n" +
"Resource Analysis & Monitoring Division\n" +
"\n" +
"Purpose - Examination of the spatial and temporal distributions of\n" +
"observations of cold-water/deep-sea corals off the west coast of the\n" +
"United States, including waters off the states of Washington, Oregon,\n" +
"and California. It is important to note that these records represent\n" +
"only presence of corals in the area swept by the trawl gear. Since\n" +
"bottom trawls used during these surveys are not designed to sample\n" +
"epibenthic invertebrates, absence of corals in the catch does not\n" +
"necessary mean they do not occupy the area swept by the trawl gear.\n" +
"\n" +
"Data Credits - NOAA Fisheries, Alaska Fisheries Science Center,\n" +
"Resource Assessment & Conservation Engineering Division (RACE) NOAA\n" +
"Fisheries, Northwest Fisheries Science Center, Fishery Resource\n" +
"Analysis & Monitoring Division (FRAM)\n" +
"\n" +
"Contact: Curt Whitmire, NOAA NWFSC, Curt.Whitmire@noaa.gov\";\n" +
" :time_coverage_end = \"2001-01-01T00:00:00Z\";\n" +
" :time_coverage_start = \"1998-01-01T00:00:00Z\";\n" +
" :title = \"NWFSC Coral Data Collected off West Coast of US (1980-2005)\";\n" +
" :Westernmost_Easting = -125.00184631347656; // double\n" +
" data:\n" +
"longitude =\n" +
"  {-121.42329406738281, -121.3140640258789, -124.56075286865234, -125.00184631347656}\n" +
"latitude =\n" +
"  {34.911720275878906, 34.911373138427734, 47.082645416259766, 47.237003326416016}\n" +
"altitude =\n" +
"  {462.0, 474.0, 77.0, 450.0}\n" +
"time =\n" +
"  {8.836128E8, 8.836128E8, 9.783072E8, 9.783072E8}\n" +
"taxa_scientific =\"Alyconaria unident.\", \"Alyconaria unident.\", \"Alyconaria unident.\", \"Alyconaria unident.\"\n" +
"institution =\"RACE\", \"RACE\", \"RACE\", \"RACE\"\n" +
"species_code =\n" +
"  {41101.0, 41101.0, 41101.0, 41101.0}\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFPoint finished.");

    }

    /**
     * Test making an .ncCF TimeSeries file.
     */
    public static void testNcCFTimeSeries() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFTimeSeries");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "longitude,latitude,station,time,atmp,wtmp" +
            "&longitude>-123&longitude<-122&latitude>37&latitude<38" +
            "&time>=2005-05-01T00&time<=2005-05-01T03", 
            EDStatic.fullTestCacheDirectory, "ncCF", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF.nc {\n" +
" dimensions:\n" +
"   timeseries = 7;\n" +
"   obs = 28;\n" +
"   stationStringLength = 5;\n" +
" variables:\n" +
"   float longitude(timeseries=7);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -122.975f, -122.21f; // float\n" +
"     :axis = \"X\";\n" +
"     :comment = \"The longitude of the station.\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float latitude(timeseries=7);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 37.363f, 37.997f; // float\n" +
"     :axis = \"Y\";\n" +
"     :comment = \"The latitude of the station.\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   char station(timeseries=7, stationStringLength=5);\n" +
"     :cf_role = \"timeseries_id\";\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Station Name\";\n" +
"   int rowSize(timeseries=7);\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Number of Observations for this TimeSeries\";\n" +
"     :sample_dimension = \"obs\";\n" +  
"   double time(obs=28);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.1149056E9, 1.1149164E9; // double\n" +
"     :axis = \"T\";\n" +
"     :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   float atmp(obs=28);\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :actual_range = 13.2f, 15.5f; // float\n" +
"     :colorBarMaximum = 40.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :comment = \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n" +
"     :coordinates = \"time latitude longitude\";\n" +
"     :ioos_category = \"Temperature\";\n" +
"     :long_name = \"Air Temperature\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"air_temperature\";\n" +
"     :units = \"degree_C\";\n" +
"   float wtmp(obs=28);\n" +
"     :_FillValue = -9999999.0f; // float\n" +
"     :actual_range = 9.3f, 17.1f; // float\n" +
"     :colorBarMaximum = 32.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
"     :coordinates = \"time latitude longitude\";\n" +
"     :ioos_category = \"Temperature\";\n" +
"     :long_name = \"SST\";\n" +
"     :missing_value = -9999999.0f; // float\n" +
"     :standard_name = \"sea_surface_temperature\";\n" +
"     :units = \"degree_C\";\n" +
"\n" +
" :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :cdm_data_type = \"TimeSeries\";\n" +
" :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
" :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :contributor_role = \"Source of data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :creator_email = \"dave.foley@noaa.gov\";\n" +
" :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
" :creator_url = \"http://coastwatch.pfeg.noaa.gov\";\n" +
" :Easternmost_Easting = -122.21f; // float\n" +
" :featureType = \"TimeSeries\";\n" + 
" :geospatial_lat_max = 37.997f; // float\n" +
" :geospatial_lat_min = 37.363f; // float\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = -122.21f; // float\n" +
" :geospatial_lon_min = -122.975f; // float\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_positive = \"down\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"NOAA NDBC\n" +
today + " http://www.ndbc.noaa.gov/\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/cwwcNDBCMet.ncCF?longitude,latitude,station,time,atmp,wtmp&longitude>-123&longitude<-122&latitude>37&latitude<38&time>=2005-05-01T00&time<=2005-05-01T03\";\n" +
" :id = \"ncCF\";\n" +
" :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :institution = \"NOAA NDBC, CoastWatch WCN\";\n" +
" :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, altitude, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
" :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
" :Northernmost_Northing = 37.997f; // float\n" +
" :project = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
" :quality = \"Automated QC checks with periodic manual QC\";\n" +
" :source = \"station observation\";\n" +
" :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
" :Southernmost_Northing = 37.363f; // float\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"station, longitude, latitude\";\n" +
" :summary = \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data (quality controlled, before\n" +
"2012-03-01T00:00:00Z) and near real time data (less quality controlled, from\n" + //changes
"2012-03-01T00:00:00Z on).\";\n" +                                                 //changes
" :time_coverage_end = \"2005-05-01T03:00:00Z\";\n" +
" :time_coverage_resolution = \"P1H\";\n" +
" :time_coverage_start = \"2005-05-01T00:00:00Z\";\n" +
" :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
" :Westernmost_Easting = -122.975f; // float\n" +
" data:\n" +
"longitude =\n" +
"  {-122.881, -122.833, -122.298, -122.465, -122.975, -122.4, -122.21}\n" +
"latitude =\n" +
"  {37.363, 37.759, 37.772, 37.807, 37.997, 37.928, 37.507}\n" +
"station =\"46012\", \"46026\", \"AAMC1\", \"FTPC1\", \"PRYC1\", \"RCMC1\", \"RTYC1\"\n" +
"rowSize =\n" +
"  {4, 4, 4, 4, 4, 4, 4}\n" +
"time =\n" +
"  {1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9}\n" +
"atmp =\n" +
"  {13.3, 13.3, 13.3, 13.3, -9999999.0, 13.3, 13.3, 13.2, 14.9, 14.5, 14.7, 14.2, 13.8, 13.9, 13.6, 13.3, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 15.5, 15.1, 14.5, 13.6, 14.7, 14.7, 15.0, 14.3}\n" +
"wtmp =\n" +
"  {13.3, 13.3, 13.4, 13.3, -9999999.0, 13.4, 13.3, 13.2, 17.1, 16.6, 15.6, 15.2, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 9.5, 9.3, -9999999.0, 9.6, 14.7, 14.7, 14.5, 14.4, 16.6, 16.6, 16.6, 16.5}\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFTimeSeries finished.");

    }


    /**
     * Test making an .ncCF TrajectoryProfile file.
     */
    public static void testNcCFTrajectoryProfile() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFTrajectoryProfile");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("erdGlobecBottle"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            //for nwioosAdcp1995
            //"yearday,longitude,latitude,altitude,eastv,northv" +
            //"&yearday>=241.995&yearday<=242", 
            //for erdGlobecBottle
            "cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0" +
            "&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z",
            EDStatic.fullTestCacheDirectory, "ncCF", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF.nc {\n" +
" dimensions:\n" +
"   trajectory = 1;\n" +
"   profile = 2;\n" +
"   obs = 13;\n" +
"   cruise_idStringLength = 6;\n" +
"   shipStringLength = 11;\n" +
" variables:\n" +
"   char cruise_id(trajectory=1, cruise_idStringLength=6);\n" +
"     :cf_role = \"trajectory_id\";\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Cruise ID\";\n" +
"   char ship(trajectory=1, shipStringLength=11);\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Ship\";\n" +
"   short cast(profile=2);\n" +
"     :actual_range = 127S, 127S; // short\n" +
"     :colorBarMaximum = 140.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Cast Number\";\n" +
"   float longitude(profile=2);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -124.3f, -124.18f; // float\n" +
"     :axis = \"X\";\n" +
"     :colorBarMaximum = -115.0; // double\n" +
"     :colorBarMinimum = -135.0; // double\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float latitude(profile=2);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 44.65f, 44.65f; // float\n" +
"     :axis = \"Y\";\n" +
"     :colorBarMaximum = 55.0; // double\n" +
"     :colorBarMinimum = 30.0; // double\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   double time(profile=2);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.02974748E9, 1.02975156E9; // double\n" +
"     :axis = \"T\";\n" +
"     :cf_role = \"profile_id\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   int trajectoryIndex(profile=2);\n" +
"     :instance_dimension = \"trajectory\";\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"The trajectory to which this profile is associated.\";\n" +
"   int rowSize(profile=2);\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Number of Observations for this Profile\";\n" +
"     :sample_dimension = \"obs\";\n" +
"   short bottle_posn(obs=13);\n" +
"     :_CoordinateAxisType = \"Height\";\n" +
"     :actual_range = 1S, 7S; // short\n" +
"     :axis = \"Z\";\n" +
"     :colorBarMaximum = 12.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Bottle Number\";\n" +
"     :missing_value = -128S; // short\n" +
"   float temperature0(obs=13);\n" +
"     :actual_range = 7.223f, 9.62f; // float\n" +
"     :colorBarMaximum = 32.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :coordinates = \"time latitude longitude bottle_posn\";\n" +
"     :ioos_category = \"Temperature\";\n" +
"     :long_name = \"Sea Water Temperature from T0 Sensor\";\n" +
"     :missing_value = -9999.0f; // float\n" +
"     :standard_name = \"sea_water_temperature\";\n" +
"     :units = \"degree_C\";\n" +
"\n" +
" :cdm_altitude_proxy = \"bottle_posn\";\n" +
" :cdm_data_type = \"TrajectoryProfile\";\n" +
" :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
" :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Easternmost_Easting = -124.18f; // float\n" +
" :featureType = \"TrajectoryProfile\";\n" +
" :geospatial_lat_max = 44.65f; // float\n" +
" :geospatial_lat_min = 44.65f; // float\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = -124.18f; // float\n" +
" :geospatial_lon_min = -124.3f; // float\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :history = \"" +
today + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle.ncCF?cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z\";\n" +
" :id = \"ncCF\";\n" +
" :infoUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/PaCOOS/GLOBEC/catalog.html?dataset=GLOBEC_Bottle_data\";\n" +
" :institution = \"GLOBEC\";\n" +
" :keywords = \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Northernmost_Northing = 44.65f; // float\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\";\n" +
" :Southernmost_Northing = 44.65f; // float\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
" :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
" :time_coverage_end = \"2002-08-19T10:06:00Z\";\n" +
" :time_coverage_start = \"2002-08-19T08:58:00Z\";\n" +
" :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
" :Westernmost_Easting = -124.3f; // float\n" +
" data:\n" +
"cruise_id =\"nh0207\"\n" +
"ship =\"New_Horizon\"\n" +
"cast =\n" +
"  {127, 127}\n" +
"longitude =\n" +
"  {-124.3, -124.18}\n" +
"latitude =\n" +
"  {44.65, 44.65}\n" +
"time =\n" +
"  {1.02974748E9, 1.02975156E9}\n" +
"trajectoryIndex =\n" +
"  {0, 0}\n" +
"rowSize =\n" +
"  {7, 6}\n" +
"bottle_posn =\n" +
"  {1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6}\n" +
"temperature0 =\n" +
"  {7.314, 7.47, 7.223, 7.962, 9.515, 9.576, 9.62, 7.378, 7.897, 7.335, 8.591, 8.693, 8.708}\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFTrajectoryProfile finished.");

    }

    /** Test speed of Data Access Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedDAF() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedDAF.txt";
        Writer writer = new FileWriter(fileName);
        tableDataset.writeDapHtmlForm(null, "", writer);

        //time it DAF
        String2.log("start timing"); 
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)  //1000 so it dominates program run time
            tableDataset.writeDapHtmlForm(null, "", writer);
        String2.log("EDDTableFromDap.testSpeedDAF time per .html = " +
            ((System.currentTimeMillis() - time) / 1000.0) + 
            "ms (java1.6 14.6ms, java1.5 40.8ms)\n" +  //slow because of info for sliders and subset variables
            "  outputFileName=" + fileName);
        EDD.testVerbose(true);
    }

    /** Test speed of Make A Graph Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedMAG() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedMAG.txt";
        String2.log("fileName=" + fileName);
        OutputStreamSource oss = new OutputStreamSourceSimple(new FileOutputStream(fileName));
        tableDataset.respondToGraphQuery(null, null, "", "", oss, null, null, null);

        //time it 
        String2.log("start timing");
        long time2 = System.currentTimeMillis();
        int n = 1000; //1000 so it dominates program run time if profiling
        for (int i = 0; i < n; i++) 
            tableDataset.respondToGraphQuery(null, null, "", "", oss,
                EDStatic.fullTestCacheDirectory, "testSpeedMAG.txt", ".graph");
        String2.log("EDDTableFromNcFiles.testSpeedMAG time per .graph = " +
            ((System.currentTimeMillis() - time2) / (float)n) + 
            "ms (java1.6 10.7ms, java1.5 55.172ms)\n" + //slow because of info for sliders and subset variables
            "  outputFileName=" + fileName);
        EDD.testVerbose(true);
    }

    /** Test speed of Subset Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedSubset() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedSubset.txt";
        String2.log("fileName=" + fileName);
        OutputStreamSource oss = new OutputStreamSourceSimple(new FileOutputStream(fileName));
        tableDataset.respondToGraphQuery(null, null, "", "", oss,
            EDStatic.fullTestCacheDirectory, "testSpeedSubset.txt", ".graph");

        //time it 
        String2.log("start timing");
        long time2 = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) //1000 so it dominates program run time
            tableDataset.respondToGraphQuery(null, null, "", "", oss,
                EDStatic.fullTestCacheDirectory, "testSpeedSubset.txt", ".graph");
        String2.log("EDDTableFromDap.testSpeedSubset time per .graph = " +
            ((System.currentTimeMillis() - time2) / 1000.0) + "ms (java1.6 17.36ms)\n" +
            "  outputFileName=" + fileName);
        EDD.testVerbose(true);
        Math2.sleep(5000);
    }

    /**
     * Test requesting float=NaN.
     */
    public static void testEqualsNaN() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testEqualsNaN");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("erdCalcofiSub"); 
        String tName, error, results, expected;

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&line=NaN&station=0",
            EDStatic.fullTestCacheDirectory, "equalsNaN", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        //String2.log(results);
        expected = 
"line_station,line,station,longitude,latitude,time,altitude,chlorophyll,dark,light_percent,NH3,NO2,NO3,oxygen,PO4,pressure,primprod,salinity,silicate,temperature\n" +
",,,degrees_east,degrees_north,UTC,m,mg m-3,mg m-3 experiment-1,mg m-3 experiment-1,ugram-atoms L-1,ugram-atoms L-1,ugram-atoms L-1,mL L-1,ugram-atoms L-1,dbar,mg m-3 experiment-1,PSU,ugram-atoms L-1,degree_C\n" +
"_000,NaN,0.0,-126.073326,39.228333,1987-05-12T12:44:00Z,-11.0,0.22,NaN,NaN,NaN,0.0,0.0,6.21,0.42,11.1,NaN,32.76,2.9,13.9\n" +
"_000,NaN,0.0,-125.53833,38.47333,1987-05-12T01:31:00Z,-12.0,0.11,NaN,NaN,NaN,0.0,0.0,6.11,0.39,12.1,NaN,32.85,3.2,14.15\n" +
"_000,NaN,0.0,-125.16833,39.31,1987-05-07T17:44:00Z,-6.0,0.16,NaN,NaN,NaN,0.0,0.0,6.23,0.43,6.3,NaN,32.66,2.0,13.17\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testEqualsNaN finished.");

    }


    /**
     * Test requesting altitude=-2.
     * This tests fix of bug where EDDTable.getSourceQueryFromDapQuery didn't use
     * altitudeMetersPerSourceUnit to convert altitude constraints to source units (e.g., /-1)!
     * 
     * <p>And this tests altitude&gt;= should become depth&lt;= internally. (and related)
     */
    public static void testAltitude() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testAltitude");

        //tests of REVERSED_OPERATOR
        EDDTable tedd; 
        String tName, error, results, expected;

        tedd = (EDDTable)oneFromDatasetXml("erdCinpKfmT"); 
        expected = 
"station,longitude,latitude,altitude,time,temperature\n" +
",degrees_east,degrees_north,m,UTC,degree_C\n" +
"Santa_Rosa_Johnsons_Lee_North,-120.1,33.883335,-11,2007-09-26T22:13:00Z,16.38\n" +
"Santa_Rosa_Johnsons_Lee_North,-120.1,33.883335,-11,2007-09-26T23:13:00Z,16.7\n";

        // >= 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude>=-17&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // > 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude>-18&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // <= 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude<=-11&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // <
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude<-10.9&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // =
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude=-11&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // !=
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude!=-10&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // =~
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude=~\"(1000|-11)\"&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);



        //*** original test
        tedd = (EDDTable)oneFromDatasetXml("epaseamapTimeSeriesProfiles"); 

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&altitude=-2",  //what we want to work
            EDStatic.fullTestCacheDirectory, "altitude", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        //String2.log(results);
        expected = 
"station_name,station,latitude,longitude,time,altitude,WaterTemperature,salinity,chlorophyll,Nitrogen,Phosphate,Ammonium\n" +
",,degrees_north,degrees_east,UTC,m,Celsius,psu,mg_m-3,percent,percent,percent\n" +
"ED16,1,29.728,-88.00584,2004-06-08T18:00:00Z,-2.0,27.2501,34.843,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testAltitude finished.");

    }

    public static void testCalcofi2() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testCalcofi2");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int epo;
        try {
            EDDTable cbio = (EDDTable)oneFromDatasetXml("erdCalcofiBio"); 
            String baseName = cbio.className() + "cbio";
            String cbioDapQuery = "&longitude>-116";

            //min max
            edv = cbio.findDataVariableByDestinationName("longitude");
            Test.ensureEqual(edv.destinationMin(), -126.4883, "");
            Test.ensureEqual(edv.destinationMax(), -115.82, "");
            edv = cbio.findDataVariableByDestinationName("latitude");
            Test.ensureEqual(edv.destinationMin(), 27.61, "");
            Test.ensureEqual(edv.destinationMax(), 37.94667, "");

            tName = cbio.makeNewFileForDapQuery(null, null, cbioDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"line_station,line,station,longitude,latitude,altitude,time,cruise,shipName,shipCode,occupy,obsCommon,obsScientific,obsValue,obsUnits\n" +
",,,degrees_east,degrees_north,m,UTC,,,,,,,,\n" +
"110_032.4,110.0,32.4,-115.82667,29.873333,-42.7,1984-11-04T04:37:00Z,8410,NEW HORIZON,NH,77,Bigmouth sole,Hippoglossina stomata,1,number of larvae\n" +
"110_032.5,110.0,32.5,-115.82,29.87,-48.9,1984-03-20T09:17:00Z,8403,DAVID STARR JORDAN,JD,777,Broadfin lampfish,Nannobrachium ritteri,1,number of larvae\n" +
"110_035,110.0,35.0,-115.995,29.785,-211.1,1984-03-20T12:06:00Z,8403,DAVID STARR JORDAN,JD,780,California flashlightfish,Protomyctophum crockeri,1,number of larvae\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            String2.log("\n\n*** GETTING DATA FROM CACHED subsetVariables data.  SHOULD BE FAST.");
            tName = cbio.makeNewFileForDapQuery(null, null, 
                "line,station,cruise,shipName&line=73.3&station=100.0&distinct()", 
                EDStatic.fullTestCacheDirectory, baseName + "_sv2", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"line,station,cruise,shipName\n" +
",,,\n" +
"73.3,100.0,302,DAVID STARR JORDAN\n" +
"73.3,100.0,304,DAVID STARR JORDAN\n" +
"73.3,100.0,404,DAVID STARR JORDAN\n" +
"73.3,100.0,8401,DAVID STARR JORDAN\n" +
"73.3,100.0,8402,NEW HORIZON\n" +
"73.3,100.0,8407,DAVID STARR JORDAN\n" +
"73.3,100.0,8410,DAVID STARR JORDAN\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
    
            // CHANGED. SEE EDDTableFromNcFiles.testCalcofi

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }
        
    } //end of testCalcofi


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
/* */
        test1D(false); //deleteCachedDatasetInfo
        test2D(true); 
        test3D(false);
        test4D(false);
        testId();
        testDistinct();
        testOrderBy();
        testOrderByMax();
        testStationLonLat();
        testStationLonLat2();
        testCalcofi();
        testCalcofi2(); //tests subset variables
        testGlobal();  //tests global: metadata to data conversion
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testErdGtsppBest();
        testTransparentPng();
        testSpeed(-1);  //15=.odv 25=png
        testManyYears();
        testCAMarCat();
        testNcCFPoint();
        testNcCFTimeSeries();
        testNcCFTrajectoryProfile();
        testSpeedDAF();
        testSpeedMAG();
        testSpeedSubset();
        testEqualsNaN();
        testAltitude();
        

        //not usually run
        //test24Hours();  //requires special set up
    }
}

