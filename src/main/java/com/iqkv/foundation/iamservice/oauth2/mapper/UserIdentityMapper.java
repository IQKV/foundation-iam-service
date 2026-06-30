package com.iqkv.foundation.iamservice.oauth2.mapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.oauth2.UserIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserIdentityMapper {

  Optional<UserIdentity> findByProviderAndProviderSub(@Param("provider") String provider,
                                                      @Param("providerSub") String providerSub);

  List<UserIdentity> findByUserId(@Param("userId") UUID userId);

  void insert(UserIdentity userIdentity);

  void updateLastUsedAt(@Param("id") UUID id);

  void deleteById(@Param("id") UUID id);

  void deleteByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
}
