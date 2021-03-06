package main.dao;

import main.datasources.worldbank.WorldBankData;
import main.domain.Country;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
/**
 *The DAO class for updating the population relation in persistent storage
 */
public class PopulationDAO extends ConnectDB{
    /**
     * This method is used to update the Countrypop relation with respect to source data. The method does not implement
     * deletion - it can only add data to the corresponding relation.
     * @param countries is a list of transfer objects
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws JAXBException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void PopulationUpdate(List<Country> countries) throws ParserConfigurationException, SAXException, IOException, JAXBException, SQLException, ClassNotFoundException {
        Connection conn = connect();
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE temp ("
                + "country_code character varying,"
                + "year numeric,"
                + "population integer,"
                + "growth_rate numeric,"
                + "mortality_rate numeric,"
                + "PRIMARY KEY(country_code,year)"
                + ");"
        );
        Iterator<Country> it = countries.iterator();
        while(it.hasNext()){
            Country count = it.next();
            List<WorldBankData> p = count.getTimeData().getPopulation();
            Iterator<WorldBankData> pIt = p.iterator();
            List<WorldBankData> g = count.getTimeData().getGrowth();
            List<WorldBankData> m = count.getTimeData().getMortality();
            while(pIt.hasNext()){
                WorldBankData pop = pIt.next();
                String country_code = pop.getCode_a2();
                int year = pop.getDate();
                int population = (int) pop.getValue();
                double growth_rate = g.stream()
                        .filter(WorldBankData -> WorldBankData.getDate()==pop.getDate())
                        .findFirst().orElse(new WorldBankData()).getValue();
                double mortality_rate = m.stream()
                        .filter(WorldBankData -> WorldBankData.getDate()==pop.getDate())
                        .findFirst().orElse(new WorldBankData()).getValue();

                st.execute("INSERT INTO temp ("
                        + "country_code,"
                        + "year,"
                        + "population,"
                        + "growth_rate,"
                        + "mortality_rate"
                        + ")"
                        + "VALUES ("
                        + "'" + country_code + "',"
                        + "'" + year + "',"
                        + "'" + population + "',"
                        + "'" + growth_rate + "',"
                        + "'" + mortality_rate + "'"
                        + ");"
                );
            }
        }
        st.execute("INSERT INTO countrypop" +
                "  (SELECT temp.country_code,temp.year,temp.population,temp.growth_rate,temp.mortality_rate" +
                "   FROM temp" +
                "   LEFT OUTER JOIN countrypop" +
                "   ON temp.country_code=countrypop.country_code" +
                "   AND temp.year=countrypop.year" +
                "   WHERE countrypop.country_code IS NULL" +
                "   OR countrypop.year IS NULL" +
                "   );" +
                "DROP TABLE temp;"
        );
        st.close();
        conn.close();
    }
}
