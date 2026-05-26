package io.sancta.sanctorum;

import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.sancta.sanctorum.dao.CityDAO;
import io.sancta.sanctorum.dao.CountryDAO;
import io.sancta.sanctorum.domain.City;
import io.sancta.sanctorum.domain.Country;
import io.sancta.sanctorum.domain.CountryLanguage;
import io.sancta.sanctorum.mapper.CityCountryMapper;
import io.sancta.sanctorum.redis.CityCountry;
import io.sancta.sanctorum.redis.Language;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.*;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GeoController {

    SessionFactory sessionFactory;
    CityDAO cityDAO;
    CountryDAO countryDAO;

    RedisClient redisClient;
    Gson gson;

    public GeoController() {
        sessionFactory = prepareRelationalDatabase();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);

        redisClient = prepareRedisClient();
        gson = new Gson().newBuilder().setPrettyPrinting().create();
    }

    public void run() {
        List<City> allCities = fetchData();
        List<CityCountry> cityCountries = transformData(allCities);
        pushToRedis(cityCountries);
        sessionFactory.getCurrentSession().close();

        benchmarkDatabasePerformance();

        shutdown();

    }

    private void benchmarkDatabasePerformance() {
        List<Integer> ids = List.of(1, 345, 899, 1233, 2344, 500, 3546, 4001, 2001, 1974);

        long startRedis = System.currentTimeMillis();
        testRedisData(ids);
        long stopRedis = System.currentTimeMillis();

        long startMysql = System.currentTimeMillis();
        testMysqlData(ids);
        long stopMysql = System.currentTimeMillis();

        System.out.println("redis: " + (stopRedis - startRedis) + " ms");
        System.out.println("MySQL: " + (stopMysql - startMysql) + " ms");

    }

    private SessionFactory prepareRelationalDatabase() {
        return new Configuration().configure().buildSessionFactory();
    }

    private RedisClient prepareRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connect = redisClient.connect()) {
            System.out.println("Connected to Redis");
        }
        return redisClient;
    }

    private void shutdown() {
        if (Objects.nonNull(sessionFactory)) sessionFactory.close();

        if (Objects.nonNull(redisClient)) redisClient.close();
    }


    private List<City> fetchData() {
        try (Session session = sessionFactory.getCurrentSession()) {
            List<City> allCities = new ArrayList<>();
            session.beginTransaction();
            List<Country> countries = countryDAO.getAll();
            int totalCount = cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                List<City> items = cityDAO.getItems(i, step);
                allCities.addAll(items);
            }
            session.getTransaction().commit();
            return allCities;
        }

    }

    private List<CityCountry> transformData(List<City> cities) {
        return CityCountryMapper.INSTANCE.citiesToCityCountries(cities);
    }

    private List<CityCountry> transformData1(List<City> cities) {
        List<CityCountry> cityCountries = new ArrayList<>();
        for (City city : cities) {
            CityCountry cityCountry = new CityCountry();
            cityCountry.setId(city.getId());
            cityCountry.setName(city.getName());
            cityCountry.setPopulation(city.getPopulation());
            cityCountry.setCountryCode(city.getCountry().getCode());
            cityCountry.setAlternativeCountryCode(city.getCountry().getAlternativeCode());
            cityCountry.setCountryName(city.getCountry().getName());
            cityCountry.setContinent(city.getCountry().getContinent());
            cityCountry.setCountryRegion(city.getCountry().getRegion());
            cityCountry.setCountrySurfaceArea(city.getCountry().getSurfaceArea());
            cityCountry.setPopulation(city.getPopulation());
            Set<CountryLanguage> countryLanguages = city.getCountry().getLanguages();
            Set<Language> languages = new HashSet<>();
            for (CountryLanguage countryLanguage : countryLanguages) {
                Language language = new Language();
                language.setLanguage(countryLanguage.getLanguage());
                language.setIsOfficial(countryLanguage.getIsOfficial());
                language.setPercentage(countryLanguage.getPercentage());
                languages.add(language);
            }
            cityCountry.setLanguages(languages);
            cityCountries.add(cityCountry);
        }
        return cityCountries;
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                sync.set(String.valueOf(cityCountry.getId()), gson.toJson(cityCountry));
            }
        }
    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                gson.fromJson(value, CityCountry.class);
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
            }
            session.getTransaction().commit();
        }
    }
}
