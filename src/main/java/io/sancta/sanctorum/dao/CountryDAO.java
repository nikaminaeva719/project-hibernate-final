package io.sancta.sanctorum.dao;

import io.sancta.sanctorum.domain.Country;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CountryDAO {

    SessionFactory sessionFactory;

    public List<Country> getAll() {
        String hql = "select country from Country as country join fetch country.languages";
        Query<Country> query = sessionFactory.getCurrentSession().createQuery(hql, Country.class);
        return query.list();
    }
}
