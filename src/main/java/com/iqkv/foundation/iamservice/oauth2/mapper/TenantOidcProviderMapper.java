package com.iqkv.foundation.iamservice.oauth2.mapper;

import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.TenantOidcProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantOidcProviderMapper {

  Optional<TenantOidcProvider> findByProviderKey(@Param("providerKey") String providerKey);

  Optional<TenantOidcProvider> findByTenantId(@Param("tenantId") UUID tenantId);

  void insert(TenantOidcProvider provider);

  void update(TenantOidcProvider provider);

  void deleteByTenantId(@Param("tenantId") UUID tenantId);
}
