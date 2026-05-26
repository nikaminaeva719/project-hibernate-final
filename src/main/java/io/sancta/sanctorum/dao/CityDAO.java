package io.sancta.sanctorum.dao;

import io.sancta.sanctorum.domain.City;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CityDAO {
    SessionFactory sessionFactory;

    public List<City> getItems(int offset, int limit) {
        String hql = "select city from City as city ";
        Query<City> query = sessionFactory.getCurrentSession().createQuery(hql, City.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    public int getTotalCount() {
        String hql = "select count(city) from City as city ";
        Query<Long> query = sessionFactory.getCurrentSession().createQuery(hql, Long.class);
        return Math.toIntExact(query.uniqueResult());
    }

    public City getById(Integer id) {
        String hql = "select city from City as city join fetch city.country where city.id = :ID";
        Query<City> query = sessionFactory.getCurrentSession().createQuery(hql, City.class);
        query.setParameter("ID",id);
        return query.getSingleResult();
    }
}
