package net.mostlyoriginal.api.plugin.extendedcomponentmapper;

import com.artemis.*;
import com.artemis.utils.reflect.ClassReflection;
import net.mostlyoriginal.api.component.common.ExtendedComponent;

/**
 * Extended Component Mapper.
 *
 * Wire support provided by {@see ExtendedComponentMapperFieldResolver}.
 *
 * @author Daan van Yperen
 */
public class M<A extends Component> {

	private final ComponentMapper<A> mapper;
	private final EntityTransmuter createTransmuter;
	private final EntityTransmuter removeTransmuter;
	private final Entity flyweight;
	private final boolean isExtendedComponent;

	@SuppressWarnings("unchecked")
	public M( Class<? extends Component> type, World world) {
		this.mapper = (ComponentMapper<A>) world.getMapper(type);
		flyweight = Entity.createFlyweight(world);
		createTransmuter = new EntityTransmuterFactory(world).add(type).build();
		removeTransmuter = new EntityTransmuterFactory(world).remove(type).build();

		isExtendedComponent = ClassReflection.isAssignableFrom(net.mostlyoriginal.api.component.common.ExtendedComponent.class, type);
	}

	public boolean isExtendedComponent() {
		return isExtendedComponent;
	}

	/**
	 * Fast and safe retrieval of a component for this entity.
	 * If the entity does not have this component then fallback is returned.
	 *
	 * @param entityId Entity that should possess the component
	 * @param fallback fallback component to return, or {@code null} to return null.
	 * @return the instance of the component
	 */
	public A getSafe(int entityId, A fallback) {
		final A c = getSafe(entityId);
		return (c != null) ? c : fallback;
	}

	/**
	 * Fast and safe retrieval of a component for this entity.
	 * If the entity does not have this component then fallback is returned.
	 *
	 * @param entity   Entity that should possess the component
	 * @param fallback fallback component to return, or {@code null} to return null.
	 * @return the instance of the component
	 */
	public A getSafe(Entity entity, A fallback) {
		return getSafe(entity.getId(), fallback);
	}

	/**
	 * Create component for this entity.
	 * Will avoid creation if component preexists.
	 *
	 * @param entityId the entity that should possess the component
	 * @return the instance of the component.
	 */
	public A create(int entityId) {
		A component = getSafe(entityId);
		if (component == null) {
			createTransmuter.transmute(asFlyweight(entityId));
			component = get(entityId);
		}
		return component;
	}

	/**
	 * Create or remove a component from an entity.
	 *
	 * Does nothing if already removed or created respectively.
	 *
	 * @param entityId Entity id to change.
	 * @param value {@code true} to create component (if missing), {@code false} to remove (if exists).
	 * @return the instance of the component, or {@code null} if removed.
	 */
	public A set(int entityId, boolean value) {
		if ( value ) {
			return create(entityId);
		} else {
			remove(entityId);
			return null;
		}
	}

	/**
	 * Mirror component between entities.
	 *
	 * 1. calls target#set(source) if source exists.
	 * 2. removes target if source is missing.
	 *
	 * Requires component to extend from {@code ExtendedComponent}.
	 *
	 * @param targetId target entity id
	 * @param sourceId source entity id
	 * @return the instance of the component, or {@code null} if removed.
	 */
	@SuppressWarnings("unchecked")
	public A mirror(int targetId, int sourceId) {
		if ( !isExtendedComponent ) {
			throw new RuntimeException("Component does not extend ExtendedComponent<T>, required for #set.");
		}

		final A source = getSafe(sourceId);
		if ( source != null ) {
			return ((ExtendedComponent<A>)create(targetId)).set(source);
		} else {
			remove(targetId);
			return null;
		}
	}

	/**
	 * Mirror component between entities.
	 *
	 * 1. calls target#set(source) if source exists.
	 * 2. removes target if source is missing.
	 *
	 * Requires component to extend from {@code ExtendedComponent}.
	 *
	 * @param target target entity
	 * @param source source entity
	 * @return the instance of the component, or {@code null} if removed.
	 */
	public A mirror(Entity target, Entity source) {
		return mirror(target.getId(), source.getId());
	}

	/**
	 * Create or remove a component from an entity.
	 *
	 * Does nothing if already removed or created respectively.
	 *
	 * @param entity Entity to change.
	 * @param value {@code true} to create component (if missing), {@code false} to remove (if exists).
	 * @return the instance of the component, or {@code null} if removed.
	 */
	public A set(Entity entity, boolean value) {
		return set(entity.getId(), value);
	}

	/**
	 * Setup flyweight with ID and return.
	 * Cannot count on just created entities being resolvable
	 * in world, which can break transmuters.
	 */
	private Entity asFlyweight(int entityId) {
		flyweight.id = entityId;
		return flyweight;
	}

	/**
	 * Remove component from entity.
	 * Does nothing if already removed.
	 *
	 * @param entityId
	 */
	public void remove(int entityId) {
		if ( has(entityId) )
		{
			removeTransmuter.transmute(asFlyweight(entityId));
		}
	}

	/**
	 * Remove component from entity.
	 * Does nothing if already removed.
	 *
	 * @param entity entity to remove.
	 */
	public void remove(Entity entity) {
		remove(entity.getId());
	}

	/**
	 * Create component for this entity.
	 * Will avoid creation if component preexists.
	 *
	 * @param entity the entity that should possess the component
	 * @return the instance of the component.
	 */
	public A create(Entity entity) {
		return create(entity.getId());
	}

	public A get(Entity e) throws ArrayIndexOutOfBoundsException {
		return mapper.get(e);
	}

	public A getSafe(Entity e, boolean forceNewInstance) {
		return mapper.getSafe(e, forceNewInstance);
	}

	public A get(int entityId) throws ArrayIndexOutOfBoundsException {
		return mapper.get(entityId);
	}

	public A getSafe(int entityId) {
		return mapper.getSafe(entityId);
	}

	public boolean has(Entity e) throws ArrayIndexOutOfBoundsException {
		return mapper.has(e);
	}

	public A getSafe(Entity e) {
		return mapper.getSafe(e);
	}

	public boolean has(int entityId) {
		return mapper.has(entityId);
	}

	public static <T extends Component> ComponentMapper<T> getFor(Class<T> type, World world) {
		return ComponentMapper.getFor(type, world);
	}

	public A get(Entity e, boolean forceNewInstance) throws ArrayIndexOutOfBoundsException {
		return mapper.get(e, forceNewInstance);
	}

	public A get(int entityId, boolean forceNewInstance) throws ArrayIndexOutOfBoundsException {
		return mapper.get(entityId, forceNewInstance);
	}

	public A getSafe(int entityId, boolean forceNewInstance) {
		return mapper.getSafe(entityId, forceNewInstance);
	}
}
