package cz.neumimto.dei.entity;

import cz.neumimto.dei.entity.database.area.ClaimedArea;
import cz.neumimto.dei.serivce.ClaimedAreaType;

import javax.persistence.MappedSuperclass;
import java.util.List;
import java.util.Set;

/**
 * Created by NeumimTo on 5.7.2016.
 */
public interface IHasClaims<T> {

    Set<T> getClaimedAreas();

    void setClaimedAreas(Set<T> areas);

    ClaimedAreaType getType();
}
