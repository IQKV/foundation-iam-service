/*
 * Copyright 2026 iQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.iamservice.infrastructure.mybatis;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * MyBatis type handler that maps a PostgreSQL {@code text[]} array column
 * (e.g. produced by {@code array_agg}) to a Java {@code List<String>}.
 */
@MappedTypes(List.class)
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

  @Override
  public void setNonNullParameter(final PreparedStatement ps, final int i,
                                  final List<String> parameter,
                                  final JdbcType jdbcType) throws SQLException {
    final Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public List<String> getNullableResult(final ResultSet rs,
                                        final String columnName) throws SQLException {
    return toList(rs.getArray(columnName));
  }

  @Override
  public List<String> getNullableResult(final ResultSet rs,
                                        final int columnIndex) throws SQLException {
    return toList(rs.getArray(columnIndex));
  }

  @Override
  public List<String> getNullableResult(final CallableStatement cs,
                                        final int columnIndex) throws SQLException {
    return toList(cs.getArray(columnIndex));
  }

  private static List<String> toList(final Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return Collections.emptyList();
    }
    final String[] arr = (String[]) sqlArray.getArray();
    return arr == null ? Collections.emptyList() : Arrays.asList(arr);
  }
}
