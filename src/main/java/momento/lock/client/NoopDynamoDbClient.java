package momento.lock.client;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BackupInUseException;
import software.amazon.awssdk.services.dynamodb.model.BackupNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ContinuousBackupsUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.CreateBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeContinuousBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeContinuousBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeEndpointsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeEndpointsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeExportRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeExportResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableSettingsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableSettingsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeImportRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeImportResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableReplicaAutoScalingRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableReplicaAutoScalingResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.model.DisableKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.DisableKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.DuplicateItemException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.EnableKinesisStreamingDestinationRequest;
import software.amazon.awssdk.services.dynamodb.model.EnableKinesisStreamingDestinationResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionResponse;
import software.amazon.awssdk.services.dynamodb.model.ExportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ExportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ExportTableToPointInTimeRequest;
import software.amazon.awssdk.services.dynamodb.model.ExportTableToPointInTimeResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IdempotentParameterMismatchException;
import software.amazon.awssdk.services.dynamodb.model.ImportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ImportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ImportTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ImportTableResponse;
import software.amazon.awssdk.services.dynamodb.model.IndexNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.InvalidExportTimeException;
import software.amazon.awssdk.services.dynamodb.model.InvalidRestoreTimeException;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionSizeLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.LimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListExportsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListExportsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListGlobalTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ListImportsRequest;
import software.amazon.awssdk.services.dynamodb.model.ListImportsResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.PointInTimeRecoveryUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReplicaAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.ReplicaNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableFromBackupRequest;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableFromBackupResponse;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableToPointInTimeRequest;
import software.amazon.awssdk.services.dynamodb.model.RestoreTableToPointInTimeResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.TableInUseException;
import software.amazon.awssdk.services.dynamodb.model.TableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TagResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.TagResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactionConflictException;
import software.amazon.awssdk.services.dynamodb.model.TransactionInProgressException;
import software.amazon.awssdk.services.dynamodb.model.UntagResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.UntagResourceResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateContinuousBackupsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateContinuousBackupsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateContributorInsightsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateContributorInsightsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableSettingsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateGlobalTableSettingsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableReplicaAutoScalingRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableReplicaAutoScalingResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveResponse;
import software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListContributorInsightsIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListExportsIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListImportsIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ListTablesIterable;
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.util.function.Consumer;

public class NoopDynamoDbClient implements DynamoDbClient {
    @Override
    public String serviceName() {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchExecuteStatementResponse batchExecuteStatement(BatchExecuteStatementRequest batchExecuteStatementRequest) throws RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchExecuteStatementResponse batchExecuteStatement(Consumer<BatchExecuteStatementRequest.Builder> batchExecuteStatementRequest) throws RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchGetItemResponse batchGetItem(BatchGetItemRequest batchGetItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchGetItemResponse batchGetItem(Consumer<BatchGetItemRequest.Builder> batchGetItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchGetItemIterable batchGetItemPaginator(BatchGetItemRequest batchGetItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchGetItemIterable batchGetItemPaginator(Consumer<BatchGetItemRequest.Builder> batchGetItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public BatchWriteItemResponse batchWriteItem(Consumer<BatchWriteItemRequest.Builder> batchWriteItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateBackupResponse createBackup(CreateBackupRequest createBackupRequest) throws TableNotFoundException, TableInUseException, ContinuousBackupsUnavailableException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateBackupResponse createBackup(Consumer<CreateBackupRequest.Builder> createBackupRequest) throws TableNotFoundException, TableInUseException, ContinuousBackupsUnavailableException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateGlobalTableResponse createGlobalTable(CreateGlobalTableRequest createGlobalTableRequest) throws LimitExceededException, InternalServerErrorException, GlobalTableAlreadyExistsException, TableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateGlobalTableResponse createGlobalTable(Consumer<CreateGlobalTableRequest.Builder> createGlobalTableRequest) throws LimitExceededException, InternalServerErrorException, GlobalTableAlreadyExistsException, TableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateTableResponse createTable(CreateTableRequest createTableRequest) throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public CreateTableResponse createTable(Consumer<CreateTableRequest.Builder> createTableRequest) throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteBackupResponse deleteBackup(DeleteBackupRequest deleteBackupRequest) throws BackupNotFoundException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteBackupResponse deleteBackup(Consumer<DeleteBackupRequest.Builder> deleteBackupRequest) throws BackupNotFoundException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteItemResponse deleteItem(DeleteItemRequest deleteItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteItemResponse deleteItem(Consumer<DeleteItemRequest.Builder> deleteItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteTableResponse deleteTable(DeleteTableRequest deleteTableRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DeleteTableResponse deleteTable(Consumer<DeleteTableRequest.Builder> deleteTableRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeBackupResponse describeBackup(DescribeBackupRequest describeBackupRequest) throws BackupNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeBackupResponse describeBackup(Consumer<DescribeBackupRequest.Builder> describeBackupRequest) throws BackupNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeContinuousBackupsResponse describeContinuousBackups(DescribeContinuousBackupsRequest describeContinuousBackupsRequest) throws TableNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeContinuousBackupsResponse describeContinuousBackups(Consumer<DescribeContinuousBackupsRequest.Builder> describeContinuousBackupsRequest) throws TableNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeContributorInsightsResponse describeContributorInsights(DescribeContributorInsightsRequest describeContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeContributorInsightsResponse describeContributorInsights(Consumer<DescribeContributorInsightsRequest.Builder> describeContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeEndpointsResponse describeEndpoints() throws AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeEndpointsResponse describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) throws AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeEndpointsResponse describeEndpoints(Consumer<DescribeEndpointsRequest.Builder> describeEndpointsRequest) throws AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeExportResponse describeExport(DescribeExportRequest describeExportRequest) throws ExportNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeExportResponse describeExport(Consumer<DescribeExportRequest.Builder> describeExportRequest) throws ExportNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeGlobalTableResponse describeGlobalTable(DescribeGlobalTableRequest describeGlobalTableRequest) throws InternalServerErrorException, GlobalTableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeGlobalTableResponse describeGlobalTable(Consumer<DescribeGlobalTableRequest.Builder> describeGlobalTableRequest) throws InternalServerErrorException, GlobalTableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeGlobalTableSettingsResponse describeGlobalTableSettings(DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) throws GlobalTableNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeGlobalTableSettingsResponse describeGlobalTableSettings(Consumer<DescribeGlobalTableSettingsRequest.Builder> describeGlobalTableSettingsRequest) throws GlobalTableNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeImportResponse describeImport(DescribeImportRequest describeImportRequest) throws ImportNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeImportResponse describeImport(Consumer<DescribeImportRequest.Builder> describeImportRequest) throws ImportNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeKinesisStreamingDestinationResponse describeKinesisStreamingDestination(DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeKinesisStreamingDestinationResponse describeKinesisStreamingDestination(Consumer<DescribeKinesisStreamingDestinationRequest.Builder> describeKinesisStreamingDestinationRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeLimitsResponse describeLimits() throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeLimitsResponse describeLimits(Consumer<DescribeLimitsRequest.Builder> describeLimitsRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTableResponse describeTable(DescribeTableRequest describeTableRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTableResponse describeTable(Consumer<DescribeTableRequest.Builder> describeTableRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTableReplicaAutoScalingResponse describeTableReplicaAutoScaling(DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTableReplicaAutoScalingResponse describeTableReplicaAutoScaling(Consumer<DescribeTableReplicaAutoScalingRequest.Builder> describeTableReplicaAutoScalingRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTimeToLiveResponse describeTimeToLive(DescribeTimeToLiveRequest describeTimeToLiveRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DescribeTimeToLiveResponse describeTimeToLive(Consumer<DescribeTimeToLiveRequest.Builder> describeTimeToLiveRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DisableKinesisStreamingDestinationResponse disableKinesisStreamingDestination(DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) throws InternalServerErrorException, LimitExceededException, ResourceInUseException, ResourceNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DisableKinesisStreamingDestinationResponse disableKinesisStreamingDestination(Consumer<DisableKinesisStreamingDestinationRequest.Builder> disableKinesisStreamingDestinationRequest) throws InternalServerErrorException, LimitExceededException, ResourceInUseException, ResourceNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public EnableKinesisStreamingDestinationResponse enableKinesisStreamingDestination(EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) throws InternalServerErrorException, LimitExceededException, ResourceInUseException, ResourceNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public EnableKinesisStreamingDestinationResponse enableKinesisStreamingDestination(Consumer<EnableKinesisStreamingDestinationRequest.Builder> enableKinesisStreamingDestinationRequest) throws InternalServerErrorException, LimitExceededException, ResourceInUseException, ResourceNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExecuteStatementResponse executeStatement(ExecuteStatementRequest executeStatementRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, DuplicateItemException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExecuteStatementResponse executeStatement(Consumer<ExecuteStatementRequest.Builder> executeStatementRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, DuplicateItemException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExecuteTransactionResponse executeTransaction(ExecuteTransactionRequest executeTransactionRequest) throws ResourceNotFoundException, TransactionCanceledException, TransactionInProgressException, IdempotentParameterMismatchException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExecuteTransactionResponse executeTransaction(Consumer<ExecuteTransactionRequest.Builder> executeTransactionRequest) throws ResourceNotFoundException, TransactionCanceledException, TransactionInProgressException, IdempotentParameterMismatchException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExportTableToPointInTimeResponse exportTableToPointInTime(ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) throws TableNotFoundException, PointInTimeRecoveryUnavailableException, LimitExceededException, InvalidExportTimeException, ExportConflictException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ExportTableToPointInTimeResponse exportTableToPointInTime(Consumer<ExportTableToPointInTimeRequest.Builder> exportTableToPointInTimeRequest) throws TableNotFoundException, PointInTimeRecoveryUnavailableException, LimitExceededException, InvalidExportTimeException, ExportConflictException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public GetItemResponse getItem(GetItemRequest getItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public GetItemResponse getItem(Consumer<GetItemRequest.Builder> getItemRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ImportTableResponse importTable(ImportTableRequest importTableRequest) throws ResourceInUseException, LimitExceededException, ImportConflictException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ImportTableResponse importTable(Consumer<ImportTableRequest.Builder> importTableRequest) throws ResourceInUseException, LimitExceededException, ImportConflictException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListBackupsResponse listBackups() throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListBackupsResponse listBackups(ListBackupsRequest listBackupsRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListBackupsResponse listBackups(Consumer<ListBackupsRequest.Builder> listBackupsRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListContributorInsightsResponse listContributorInsights(ListContributorInsightsRequest listContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListContributorInsightsResponse listContributorInsights(Consumer<ListContributorInsightsRequest.Builder> listContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListContributorInsightsIterable listContributorInsightsPaginator(ListContributorInsightsRequest listContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListContributorInsightsIterable listContributorInsightsPaginator(Consumer<ListContributorInsightsRequest.Builder> listContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListExportsResponse listExports(ListExportsRequest listExportsRequest) throws LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListExportsResponse listExports(Consumer<ListExportsRequest.Builder> listExportsRequest) throws LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListExportsIterable listExportsPaginator(ListExportsRequest listExportsRequest) throws LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListExportsIterable listExportsPaginator(Consumer<ListExportsRequest.Builder> listExportsRequest) throws LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListGlobalTablesResponse listGlobalTables() throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListGlobalTablesResponse listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListGlobalTablesResponse listGlobalTables(Consumer<ListGlobalTablesRequest.Builder> listGlobalTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListImportsResponse listImports(ListImportsRequest listImportsRequest) throws LimitExceededException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListImportsResponse listImports(Consumer<ListImportsRequest.Builder> listImportsRequest) throws LimitExceededException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListImportsIterable listImportsPaginator(ListImportsRequest listImportsRequest) throws LimitExceededException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListImportsIterable listImportsPaginator(Consumer<ListImportsRequest.Builder> listImportsRequest) throws LimitExceededException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesResponse listTables() throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesResponse listTables(ListTablesRequest listTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesResponse listTables(Consumer<ListTablesRequest.Builder> listTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesIterable listTablesPaginator() throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesIterable listTablesPaginator(ListTablesRequest listTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTablesIterable listTablesPaginator(Consumer<ListTablesRequest.Builder> listTablesRequest) throws InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTagsOfResourceResponse listTagsOfResource(ListTagsOfResourceRequest listTagsOfResourceRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ListTagsOfResourceResponse listTagsOfResource(Consumer<ListTagsOfResourceRequest.Builder> listTagsOfResourceRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public PutItemResponse putItem(PutItemRequest putItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public PutItemResponse putItem(Consumer<PutItemRequest.Builder> putItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public QueryResponse query(QueryRequest queryRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public QueryResponse query(Consumer<QueryRequest.Builder> queryRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public QueryIterable queryPaginator(QueryRequest queryRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public QueryIterable queryPaginator(Consumer<QueryRequest.Builder> queryRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public RestoreTableFromBackupResponse restoreTableFromBackup(RestoreTableFromBackupRequest restoreTableFromBackupRequest) throws TableAlreadyExistsException, TableInUseException, BackupNotFoundException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public RestoreTableFromBackupResponse restoreTableFromBackup(Consumer<RestoreTableFromBackupRequest.Builder> restoreTableFromBackupRequest) throws TableAlreadyExistsException, TableInUseException, BackupNotFoundException, BackupInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public RestoreTableToPointInTimeResponse restoreTableToPointInTime(RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) throws TableAlreadyExistsException, TableNotFoundException, TableInUseException, LimitExceededException, InvalidRestoreTimeException, PointInTimeRecoveryUnavailableException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public RestoreTableToPointInTimeResponse restoreTableToPointInTime(Consumer<RestoreTableToPointInTimeRequest.Builder> restoreTableToPointInTimeRequest) throws TableAlreadyExistsException, TableNotFoundException, TableInUseException, LimitExceededException, InvalidRestoreTimeException, PointInTimeRecoveryUnavailableException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ScanResponse scan(ScanRequest scanRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ScanResponse scan(Consumer<ScanRequest.Builder> scanRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ScanIterable scanPaginator(ScanRequest scanRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public ScanIterable scanPaginator(Consumer<ScanRequest.Builder> scanRequest) throws ProvisionedThroughputExceededException, ResourceNotFoundException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest) throws LimitExceededException, ResourceNotFoundException, InternalServerErrorException, ResourceInUseException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TagResourceResponse tagResource(Consumer<TagResourceRequest.Builder> tagResourceRequest) throws LimitExceededException, ResourceNotFoundException, InternalServerErrorException, ResourceInUseException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TransactGetItemsResponse transactGetItems(TransactGetItemsRequest transactGetItemsRequest) throws ResourceNotFoundException, TransactionCanceledException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TransactGetItemsResponse transactGetItems(Consumer<TransactGetItemsRequest.Builder> transactGetItemsRequest) throws ResourceNotFoundException, TransactionCanceledException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TransactWriteItemsResponse transactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) throws ResourceNotFoundException, TransactionCanceledException, TransactionInProgressException, IdempotentParameterMismatchException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public TransactWriteItemsResponse transactWriteItems(Consumer<TransactWriteItemsRequest.Builder> transactWriteItemsRequest) throws ResourceNotFoundException, TransactionCanceledException, TransactionInProgressException, IdempotentParameterMismatchException, ProvisionedThroughputExceededException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UntagResourceResponse untagResource(UntagResourceRequest untagResourceRequest) throws LimitExceededException, ResourceNotFoundException, InternalServerErrorException, ResourceInUseException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UntagResourceResponse untagResource(Consumer<UntagResourceRequest.Builder> untagResourceRequest) throws LimitExceededException, ResourceNotFoundException, InternalServerErrorException, ResourceInUseException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateContinuousBackupsResponse updateContinuousBackups(UpdateContinuousBackupsRequest updateContinuousBackupsRequest) throws TableNotFoundException, ContinuousBackupsUnavailableException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateContinuousBackupsResponse updateContinuousBackups(Consumer<UpdateContinuousBackupsRequest.Builder> updateContinuousBackupsRequest) throws TableNotFoundException, ContinuousBackupsUnavailableException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateContributorInsightsResponse updateContributorInsights(UpdateContributorInsightsRequest updateContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateContributorInsightsResponse updateContributorInsights(Consumer<UpdateContributorInsightsRequest.Builder> updateContributorInsightsRequest) throws ResourceNotFoundException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateGlobalTableResponse updateGlobalTable(UpdateGlobalTableRequest updateGlobalTableRequest) throws InternalServerErrorException, GlobalTableNotFoundException, ReplicaAlreadyExistsException, ReplicaNotFoundException, TableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateGlobalTableResponse updateGlobalTable(Consumer<UpdateGlobalTableRequest.Builder> updateGlobalTableRequest) throws InternalServerErrorException, GlobalTableNotFoundException, ReplicaAlreadyExistsException, ReplicaNotFoundException, TableNotFoundException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateGlobalTableSettingsResponse updateGlobalTableSettings(UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) throws GlobalTableNotFoundException, ReplicaNotFoundException, IndexNotFoundException, LimitExceededException, ResourceInUseException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateGlobalTableSettingsResponse updateGlobalTableSettings(Consumer<UpdateGlobalTableSettingsRequest.Builder> updateGlobalTableSettingsRequest) throws GlobalTableNotFoundException, ReplicaNotFoundException, IndexNotFoundException, LimitExceededException, ResourceInUseException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateItemResponse updateItem(Consumer<UpdateItemRequest.Builder> updateItemRequest) throws ConditionalCheckFailedException, ProvisionedThroughputExceededException, ResourceNotFoundException, ItemCollectionSizeLimitExceededException, TransactionConflictException, RequestLimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTableResponse updateTable(UpdateTableRequest updateTableRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTableResponse updateTable(Consumer<UpdateTableRequest.Builder> updateTableRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTableReplicaAutoScalingResponse updateTableReplicaAutoScaling(UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) throws ResourceNotFoundException, ResourceInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTableReplicaAutoScalingResponse updateTableReplicaAutoScaling(Consumer<UpdateTableReplicaAutoScalingRequest.Builder> updateTableReplicaAutoScalingRequest) throws ResourceNotFoundException, ResourceInUseException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTimeToLiveResponse updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public UpdateTimeToLiveResponse updateTimeToLive(Consumer<UpdateTimeToLiveRequest.Builder> updateTimeToLiveRequest) throws ResourceInUseException, ResourceNotFoundException, LimitExceededException, InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }

    @Override
    public DynamoDbWaiter waiter() {
        throw new UnsupportedOperationException("This is a NoOp client and this operation isn't supported");
    }
}
