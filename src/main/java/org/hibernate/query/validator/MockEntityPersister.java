package org.hibernate.query.validator;

import org.hibernate.EntityMode;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.DiscriminatorMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.ClassType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import javax.persistence.AccessType;
import java.io.Serializable;
import java.util.*;

import static org.hibernate.query.validator.MockSessionFactory.typeHelper;

public abstract class MockEntityPersister implements EntityPersister, Queryable, DiscriminatorMetadata {

    private static final String[] ID_COLUMN = {"id"};

    private final String entityName;
    private final MockSessionFactory factory;
    private final List<MockEntityPersister> subclassPersisters = new ArrayList<>();
    final AccessType defaultAccessType;
    private final Map<String,Type> propertyTypesByName = new HashMap<>();

    public MockEntityPersister(String entityName,
                        AccessType defaultAccessType,
                        MockSessionFactory factory) {
        this.entityName = entityName;
        this.factory = factory;
        this.defaultAccessType = defaultAccessType;
    }

    void initSubclassPersisters() {
        for (MockEntityPersister other: factory.getMockEntityPersisters()) {
            other.addPersister(this);
            this.addPersister(other);
        }
    }

    private void addPersister(MockEntityPersister entityPersister) {
        if (isSubclassPersister(entityPersister)) {
            subclassPersisters.add(entityPersister);
        }
    }

    private Type getSubclassPropertyType(String propertyPath) {
        return subclassPersisters.stream()
                .map(sp -> sp.getPropertyType(propertyPath))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    abstract boolean isSubclassPersister(MockEntityPersister entityPersister);

    @Override
    public SessionFactoryImplementor getFactory() {
        return factory;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public final Type getPropertyType(String propertyPath) {
        Type result = propertyTypesByName.get(propertyPath);
        if (result!=null) {
            return result;
        }

        result = createPropertyType(propertyPath);
        if (result == null) {
            //check subclasses, needed for treat()
            result = getSubclassPropertyType(propertyPath);
        }

        if (result!=null) {
            propertyTypesByName.put(propertyPath, result);
        }
        return result;
    }

    abstract Type createPropertyType(String propertyPath);

    @Override
    public Type getIdentifierType() {
        //TODO: propertyType(getIdentifierPropertyName())
        return StandardBasicTypes.INTEGER;
    }

    @Override
    public String getIdentifierPropertyName() {
        //TODO!!!!!!
        return "id";
    }

    @Override
    public Type toType(String propertyName) throws QueryException {
        Type type = getPropertyType(propertyName);
        if (type == null) {
            throw new QueryException(getEntityName()
                    + " has no mapped "
                    + propertyName);
        }
        return type;
    }

    @Override
    public String getRootEntityName() {
        return entityName;
    }

    @Override
    public Declarer getSubclassPropertyDeclarer(String s) {
        return Declarer.CLASS;
    }

    @Override
    public String[] toColumns(String alias, String propertyName)
            throws QueryException {
        return new String[] { "" };
    }

    @Override
    public String[] toColumns(String propertyName) {
        return new String[] { "" };
    }

    @Override
    public Type getType() {
        return typeHelper.entity(entityName);
    }

    @Override
    public Serializable[] getPropertySpaces() {
        return new Serializable[] {entityName};
    }

    @Override
    public Serializable[] getQuerySpaces() {
        return new Serializable[] {entityName};
    }

    @Override
    public EntityMode getEntityMode() {
        return EntityMode.POJO;
    }

    @Override
    public EntityPersister getEntityPersister() {
        return this;
    }

    @Override
    public SelectFragment propertySelectFragmentFragment(String alias, String suffix, boolean b) {
        return new SelectFragment();
    }

    @Override
    public String[] getKeyColumnNames() {
        return getIdentifierColumnNames();
    }

    @Override
    public String[] getIdentifierColumnNames() {
        return ID_COLUMN;
    }

    @Override
    public DiscriminatorMetadata getTypeDiscriminatorMetadata() {
        return this;
    }

    @Override
    public Type getResolutionType() {
        return ClassType.INSTANCE;
    }

    @Override
    public String getTableName() {
        return entityName;
    }

    @Override
    public String toString() {
        return "MockEntityPersister[" + entityName + "]";
    }

    @Override
    public int getVersionProperty() {
        return -66;
    }

    @Override
    public String getMappedSuperclass() {
        return null;
    }

    @Override
    public boolean consumesEntityAlias() {
        return true;
    }

}
