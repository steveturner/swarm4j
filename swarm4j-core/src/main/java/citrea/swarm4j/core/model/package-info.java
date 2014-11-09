/**
 * Swarm data models.
 *
 * CRDT-types:
 * <ul>
 *     <li>Syncable - base class for all op-based models</li>
 *     <li>Model - plain model with Last-Write-Wins merge strategy</li>
 *     <li>Set - set of entries which are Syncable</li>
 *     <li>Vector - ordered list of Syncable, implements three-way merge</li>
 * </ul>
 *
 * Created with IntelliJ IDEA.
 * @author aleksisha
 * Date: 09.11.2014
 * Time: 01:51
 */
package citrea.swarm4j.core.model;