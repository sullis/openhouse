package com.linkedin.openhouse.tables.mock.mapper;

import static com.linkedin.openhouse.tables.model.TableModelConstants.*;

import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;
import com.linkedin.openhouse.tables.api.spec.v0.request.components.Policies;
import com.linkedin.openhouse.tables.dto.mapper.iceberg.PoliciesSpecMapper;
import com.linkedin.openhouse.tables.model.TableDto;
import com.linkedin.openhouse.tables.model.TableModelConstants;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PoliciesSpecMapperTest {

  @Autowired protected PoliciesSpecMapper policiesMapper;

  @Test
  public void testToPoliciesSpecJson() {
    TableDto tableDto =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY
                .toBuilder()
                .policies(TableModelConstants.TABLE_POLICIES)
                .build());
    String policiesSpec = policiesMapper.toPoliciesJsonString(tableDto);
    Assertions.assertEquals(
        (Integer) JsonPath.read(policiesSpec, "$.retention.count"),
        TableModelConstants.TABLE_POLICIES.getRetention().getCount());
    Assertions.assertEquals(
        JsonPath.read(policiesSpec, "$.replication.config[0].destination"),
        TableModelConstants.TABLE_POLICIES.getReplication().getConfig().get(0).getDestination());
    Assertions.assertEquals(
        JsonPath.read(policiesSpec, "$.replication.config[0].interval"),
        TableModelConstants.TABLE_POLICIES.getReplication().getConfig().get(0).getInterval());
    Assertions.assertEquals(
        (Integer) JsonPath.read(policiesSpec, "$.history.maxAge"),
        TABLE_POLICIES.getHistory().getMaxAge());
    Assertions.assertEquals(
        JsonPath.read(policiesSpec, "$.history.granularity"),
        TABLE_POLICIES.getHistory().getGranularity().toString());
    Assertions.assertEquals(
        (Integer) JsonPath.read(policiesSpec, "$.history.versions"),
        TABLE_POLICIES.getHistory().getVersions());
  }

  @Test
  public void testFromPoliciesSpecJsonEscapedUnicode() {
    TableDto tableDto =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY
                .toBuilder()
                .policies(TableModelConstants.TABLE_POLICIES_COMPLEX)
                .build());

    String policiesSpec = policiesMapper.toPoliciesJsonString(tableDto);
    Assertions.assertEquals(
        policiesSpec.toCharArray().length, TABLE_POLICIES_COMPLEX_STRING.toCharArray().length);
    Assertions.assertArrayEquals(
        policiesSpec.toCharArray(), TABLE_POLICIES_COMPLEX_STRING.toCharArray());

    Policies policies = policiesMapper.toPoliciesObject(TABLE_POLICIES_COMPLEX_STRING);
    Assertions.assertEquals(policies, TableModelConstants.TABLE_POLICIES_COMPLEX);
  }

  @Test
  public void testToPolicyObjectFromJsonEscapedUnicode() {
    Policies policies = policiesMapper.toPoliciesObject(TABLE_POLICIES_COMPLEX_STRING);
    Assertions.assertEquals(policies, TableModelConstants.TABLE_POLICIES_COMPLEX);
    Assertions.assertEquals(
        policies.getRetention().getCount(),
        GET_TABLE_RESPONSE_BODY.getPolicies().getRetention().getCount());

    // Backwards compatibility, deserializing without the disableHtmlEscaping() gson option
    String policyWithEscapedChars =
        new GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(TableModelConstants.TABLE_POLICIES_COMPLEX);
    policies = policiesMapper.toPoliciesObject(policyWithEscapedChars);
    Assertions.assertEquals(policies, TableModelConstants.TABLE_POLICIES_COMPLEX);
    Assertions.assertEquals(
        policies.getRetention().getCount(),
        GET_TABLE_RESPONSE_BODY.getPolicies().getRetention().getCount());
  }

  @Test
  public void testToPoliciesSpecJsonWithNullPolicies() {
    TableDto tableDtoWithNullPolicies =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY.toBuilder().policies(null).build());
    String policiesSpec = policiesMapper.toPoliciesJsonString(tableDtoWithNullPolicies);
    Assertions.assertEquals(policiesSpec, "");
  }

  @Test
  public void testToPoliciesJsonFromObject() {
    TableDto tableDto =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY
                .toBuilder()
                .policies(TableModelConstants.TABLE_POLICIES)
                .build());
    String jsonPolicies = policiesMapper.toPoliciesJsonString(tableDto);
    Assertions.assertEquals(3, (Integer) JsonPath.read(jsonPolicies, "$.retention.count"));
  }

  @Test
  public void testToPoliciesJsonWithLockFromObject() {
    TableDto tableDto =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY.toBuilder().policies(TABLE_POLICIES_WITH_LOCK_STATUS).build());
    String jsonPolicies = policiesMapper.toPoliciesJsonString(tableDto);
    Assertions.assertTrue((boolean) JsonPath.read(jsonPolicies, "$.lockState.locked"));
  }

  @Test
  public void testEmptyPoliciesJsonFromObjectWithNullPolicy() {
    TableDto tableDto =
        TableModelConstants.buildTableDto(
            GET_TABLE_RESPONSE_BODY.toBuilder().policies(null).build());

    String jsonPolicies = policiesMapper.toPoliciesJsonString(tableDto);
    Assertions.assertEquals("", jsonPolicies);
  }

  @Test
  public void testToPolicyObjectFromJson() {
    Policies policies = policiesMapper.toPoliciesObject(TABLE_POLICIES_COMPLEX_STRING);
    Assertions.assertEquals(policies, TABLE_POLICIES_COMPLEX);
    Assertions.assertEquals(
        policies.getRetention().getCount(),
        GET_TABLE_RESPONSE_BODY.getPolicies().getRetention().getCount());
  }

  private static String getBadJsonString() {
    JsonNodeFactory factory = JsonNodeFactory.instance;
    ObjectNode node = factory.objectNode();
    ObjectNode retention = factory.objectNode();
    retention.put("days", 3);
    node.put("rention", retention);
    ObjectNode nodePolicies = factory.objectNode();
    nodePolicies.put("policies", node);
    return node.toString();
  }

  @Test
  public void testErrorPolicyObjectFromJson() {
    Policies policies = policiesMapper.toPoliciesObject(getBadJsonString());
    Assertions.assertNotNull(policies);
    Assertions.assertNull(policies.getRetention());
  }
}
