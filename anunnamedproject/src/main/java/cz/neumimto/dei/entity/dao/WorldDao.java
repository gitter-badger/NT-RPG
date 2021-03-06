package cz.neumimto.dei.entity.dao;

import cz.neumimto.core.dao.genericDao.GenericDao;

import cz.neumimto.dei.entity.database.area.TownClaim;
import org.hibernate.Query;
import org.hibernate.Session;
import org.spongepowered.api.event.Event;

import java.util.List;


/**
 * Created by NeumimTo on 6.7.2016.
 */
public class WorldDao extends GenericDao {

    public List<TownClaim> loadWorldTowns(String world) {
        Session session = factory.openSession();
        Query query = session.createQuery("from TownClaim tc where tc.world=:world");
        query.setParameter("world",world);
        List list = query.list();
        session.close();
        return list;
    }

    public List<TownClaim> loadStrongholdClaims(String world) {
        return null;
    }
}
