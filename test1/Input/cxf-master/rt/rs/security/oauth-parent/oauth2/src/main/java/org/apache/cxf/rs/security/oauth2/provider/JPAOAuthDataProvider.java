/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.oauth2.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;

/**
 * Provides a Jpa BMT implementation for OAuthDataProvider.
 *
 * If your application runs in a container and if you want to use
 * container managed persistence, you'll have to override
 * the following methods :
 * <ul>
 *     <li> {@link #getEntityManager()}</li>
 *     <li> {@link #commitIfNeeded(EntityManager)}</li>
 *     <li> {@link #closeIfNeeded(EntityManager)}</li>
 * </ul>
 */
public class JPAOAuthDataProvider extends AbstractOAuthDataProvider {
    private static final String CLIENT_QUERY = "SELECT client FROM Client client"
            + " INNER JOIN client.resourceOwnerSubject ros";

    private EntityManagerFactory entityManagerFactory;

    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.entityManagerFactory = emf;
    }

    @Override
    public Client doGetClient(final String clientId) throws OAuthServiceException {
        return execute(new EntityManagerOperation<Client>() {
            @Override
            public Client execute(EntityManager em) {
                return em.find(Client.class, clientId);
            }
        });
    }

    protected <T> T execute(EntityManagerOperation<T> operation) {
        EntityManager em = getEntityManager();
        T value;
        try {
            value = operation.execute(em);
        } finally {
            closeIfNeeded(em);
        }
        return value;
    }

    protected <T> T executeInTransaction(EntityManagerOperation<T> operation) {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = null;
        T value;
        try {
            transaction = beginIfNeeded(em);
            value = operation.execute(em);
            flushIfNeeded(em);
            commitIfNeeded(em);
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            closeIfNeeded(em);
        }
        return value;
    }

    public void setClient(final Client client) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                if (client.getResourceOwnerSubject() != null) {
                    UserSubject sub =
                            em.find(UserSubject.class, client.getResourceOwnerSubject().getId());
                    if (sub == null) {
                        em.persist(client.getResourceOwnerSubject());
                    } else {
                        client.setResourceOwnerSubject(sub);
                    }
                }
                boolean clientExists = em.createQuery("SELECT count(client) from Client client "
                                + "where client.clientId = :id", Long.class)
                        .setParameter("id", client.getClientId())
                        .getSingleResult() > 0;
                if (clientExists) {
                    em.merge(client);
                } else {
                    em.persist(client);
                }
                return null;
            }
        });
    }

    @Override
    protected void doRemoveClient(final Client c) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                Client clientToRemove = em.getReference(Client.class, c.getClientId());
                em.remove(clientToRemove);
                return null;
            }
        });
    }

    @Override
    public List<Client> getClients(final UserSubject resourceOwner) {
        return execute(new EntityManagerOperation<List<Client>>() {
            @Override
            public List<Client> execute(EntityManager em) {
                return getClientsQuery(resourceOwner, em).getResultList();
            }
        });
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(final Client c, final UserSubject sub) {
        return execute(new EntityManagerOperation<List<ServerAccessToken>>() {
            @Override
            public List<ServerAccessToken> execute(EntityManager em) {
                return CastUtils.cast(getTokensQuery(c, sub, em).getResultList());
            }
        });
    }

    @Override
    public List<RefreshToken> getRefreshTokens(final Client c, final UserSubject sub) {
        return execute(new EntityManagerOperation<List<RefreshToken>>() {
            @Override
            public List<RefreshToken> execute(EntityManager em) {
                return getRefreshTokensQuery(c, sub, em).getResultList();
            }
        });
    }

    @Override
    public ServerAccessToken getAccessToken(final String accessToken) throws OAuthServiceException {
        return execute(new EntityManagerOperation<ServerAccessToken>() {
            @Override
            public ServerAccessToken execute(EntityManager em) {
                return em.find(BearerAccessToken.class, accessToken);
            }
        });
    }

    @Override
    protected void doRevokeAccessToken(final ServerAccessToken at) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                ServerAccessToken tokenToRemove = em.getReference(at.getClass(), at.getTokenKey());
                em.remove(tokenToRemove);
                return null;
            }
        });
    }

    @Override
    protected void linkRefreshTokenToAccessToken(final RefreshToken rt, final ServerAccessToken at) {
        super.linkRefreshTokenToAccessToken(rt, at);
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                em.merge(at);
                return null;
            }
        });
    }

    @Override
    protected RefreshToken getRefreshToken(final String refreshTokenKey) {
        return execute(new EntityManagerOperation<RefreshToken>() {
            @Override
            public RefreshToken execute(EntityManager em) {
                return em.find(RefreshToken.class, refreshTokenKey);
            }
        });
    }

    @Override
    protected void doRevokeRefreshToken(final RefreshToken rt) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                RefreshToken tokentoRemove = em.getReference(RefreshToken.class, rt.getTokenKey());
                em.remove(tokentoRemove);
                return null;
            }
        });
    }

    @Override
    protected ServerAccessToken doCreateAccessToken(AccessTokenRegistration atReg) {
        ServerAccessToken at = super.doCreateAccessToken(atReg);
        // we override this in order to get rid of elementCollections directly injected
        // from another entity
        // this can be the case when using multiple cmt dataProvider operation in a single entityManager
        // lifespan
        if (at.getAudiences() != null) {
            at.setAudiences(new ArrayList<>(at.getAudiences()));
        }
        if (at.getExtraProperties() != null) {
            at.setExtraProperties(new HashMap<String, String>(at.getExtraProperties()));
        }
        if (at.getScopes() != null) {
            at.setScopes(new ArrayList<>(at.getScopes()));
        }
        if (at.getParameters() != null) {
            at.setParameters(new HashMap<String, String>(at.getParameters()));
        }
        return at;
    }

    protected void saveAccessToken(final ServerAccessToken serverToken) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                List<OAuthPermission> perms = new LinkedList<OAuthPermission>();
                for (OAuthPermission perm : serverToken.getScopes()) {
                    OAuthPermission permSaved = em.find(OAuthPermission.class, perm.getPermission());
                    if (permSaved != null) {
                        perms.add(permSaved);
                    } else {
                        em.persist(perm);
                        perms.add(perm);
                    }
                }
                serverToken.setScopes(perms);

                if (serverToken.getSubject() != null) {
                    UserSubject sub = em.find(UserSubject.class, serverToken.getSubject().getId());
                    if (sub == null) {
                        em.persist(serverToken.getSubject());
                    } else {
                        sub = em.merge(serverToken.getSubject());
                        serverToken.setSubject(sub);
                    }
                }
                // ensure we have a managed association
                // (needed for OpenJPA : InvalidStateException: Encountered unmanaged object)
                if (serverToken.getClient() != null) {
                    serverToken.setClient(em.find(Client.class, serverToken.getClient().getClientId()));
                }

                em.persist(serverToken);
                return null;
            }
        });
    }

    protected void saveRefreshToken(RefreshToken refreshToken) {
        persistEntity(refreshToken);
    }

    protected void persistEntity(final Object entity) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                em.persist(entity);
                return null;
            }
        });
    }

    protected void removeEntity(final Object entity) {
        executeInTransaction(new EntityManagerOperation<Void>() {
            @Override
            public Void execute(EntityManager em) {
                em.remove(entity);
                return null;
            }
        });
    }

    protected TypedQuery<Client> getClientsQuery(UserSubject resourceOwnerSubject, EntityManager entityManager) {
        if (resourceOwnerSubject == null) {
            return entityManager.createQuery(CLIENT_QUERY, Client.class);
        } else {
            return entityManager.createQuery(CLIENT_QUERY + " WHERE ros.login = :login", Client.class).
                    setParameter("login", resourceOwnerSubject.getLogin());
        }
    }

    protected TypedQuery<BearerAccessToken> getTokensQuery(Client c, UserSubject resourceOwnerSubject,
                                                           EntityManager entityManager) {
        if (c == null && resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT t FROM BearerAccessToken t", BearerAccessToken.class);
        } else if (c == null) {
            return entityManager.createQuery(
                    "SELECT t FROM BearerAccessToken t"
                            + " JOIN t.subject s"
                            + " WHERE s.login = :login", BearerAccessToken.class)
                    .setParameter("login", resourceOwnerSubject.getLogin());
        } else if (resourceOwnerSubject == null) {
            return entityManager.createQuery(
                    "SELECT t FROM BearerAccessToken t"
                            + " JOIN t.client c"
                            + " WHERE c.clientId = :clientId", BearerAccessToken.class)
                    .setParameter("clientId", c.getClientId());
        } else {
            return entityManager.createQuery(
                    "SELECT t FROM BearerAccessToken t"
                            + " JOIN t.subject s"
                            + " JOIN t.client c"
                            + " WHERE s.login = :login AND c.clientId = :clientId", BearerAccessToken.class)
                    .setParameter("login", resourceOwnerSubject.getLogin())
                    .setParameter("clientId", c.getClientId());
        }
    }

    protected TypedQuery<RefreshToken> getRefreshTokensQuery(Client c, UserSubject resourceOwnerSubject,
                                                             EntityManager entityManager) {
        if (c == null && resourceOwnerSubject == null) {
            return entityManager.createQuery("SELECT t FROM RefreshToken t", RefreshToken.class);
        } else if (c == null) {
            return entityManager.createQuery(
                    "SELECT t FROM RefreshToken t"
                            + " JOIN t.subject s"
                            + " WHERE s.login = :login", RefreshToken.class)
                    .setParameter("login", resourceOwnerSubject.getLogin());
        } else if (resourceOwnerSubject == null) {
            return entityManager.createQuery(
                    "SELECT t FROM RefreshToken t"
                            + " JOIN t.client c"
                            + " WHERE c.clientId = :clientId", RefreshToken.class)
                    .setParameter("clientId", c.getClientId());
        } else {
            return entityManager.createQuery(
                    "SELECT t FROM RefreshToken t"
                            + " JOIN t.subject s"
                            + " JOIN t.client c"
                            + " WHERE s.login = :login AND c.clientId = :clientId", RefreshToken.class)
                    .setParameter("login", resourceOwnerSubject.getLogin())
                    .setParameter("clientId", c.getClientId());
        }
    }

    /**
     * Returns the entityManaged used for the current operation.
     */
    protected EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Begins the current transaction.
     *
     * This method needs to be overridden in a CMT environment.
     */
    protected EntityTransaction beginIfNeeded(EntityManager em) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        return tx;
    }

    /**
     * Flush the current transaction.
     */
    protected void flushIfNeeded(EntityManager em) {
        em.flush();
    }

    /**
     * Commits the current transaction.
     *
     * This method needs to be overridden in a CMT environment.
     */
    protected void commitIfNeeded(EntityManager em) {
        em.getTransaction().commit();
    }

    /**
     * Closes the current em.
     *
     * This method needs to be overriden in a CMT environment.
     */
    protected void closeIfNeeded(EntityManager em) {
        em.close();
    }

    public interface EntityManagerOperation<T> {
        T execute(EntityManager em);
    }
}
