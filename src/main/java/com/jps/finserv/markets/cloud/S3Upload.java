package com.jps.finserv.markets.cloud;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.file.Paths;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Upload {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(S3Upload.class);

	public void run(String pathUploadSrc, boolean recursive){
		String bucket_name = "finservbucket";
		boolean cleanUpload = false;

		if ((pathUploadSrc != null)&&(!pathUploadSrc.equals("")))
		{
			logger.info("\nCreating S3 bucket: %s\n", bucket_name);
			Bucket b = createBucket(bucket_name);
			final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
			if (b == null) {
				logger.info("Error creating bucket!\n");
			} else {
				logger.info("Done!\n");
				// TODO: Currently not set to recursively go through directory structure regardless of "recursive" flag 
				List<File> listFiles = listFilesToUpload(pathUploadSrc, recursive);
				for (File f : listFiles) {
					String path = f.getAbsolutePath();
					// TODO: Only uploading files, not handling recursive directories right now
					if (!f.isDirectory()){
						boolean upload = putObject(bucket_name, path);
						if (upload)
							cleanUpload = upload; 
					}
				}
			}

			// If any file was uploaded then list files in the bucket
			if (cleanUpload) {
				ListObjectsV2Result result = s3.listObjectsV2(bucket_name);
				List<S3ObjectSummary> objects = result.getObjectSummaries();
				for (S3ObjectSummary os: objects) {
					logger.info("* " + os.getKey());
				}
			}
			else
				logger.error("No new file uploaded into the bucket.");
		}
	}

	private Bucket createBucket(String bucket_name) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		Bucket b = null;
		
		if (s3.doesBucketExistV2(bucket_name)) {
			logger.info("Bucket %s already exists.\n", bucket_name);
			b = getBucket(bucket_name);
		} else {
			try {
				b = s3.createBucket(bucket_name);
			} catch (AmazonS3Exception e) {
				logger.error(e.getErrorMessage());
			}
		}
		return b;
	}

	private Bucket getBucket(String bucket_name) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		Bucket named_bucket = null;
		List<Bucket> buckets = s3.listBuckets();
		for (Bucket b : buckets) {
			if (b.getName().equals(bucket_name)) {
				named_bucket = b;
			}
		}
		return named_bucket;
	}

	private static boolean putObject(String bucket_name, String fileName) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		File file = new File(fileName);
		String key_name = Paths.get(fileName).getFileName().toString();

		logger.info("Uploading %s to S3 bucket %s...\n", fileName, bucket_name);
		try {
			PutObjectResult putObjectResult = s3.putObject(bucket_name, key_name, file);
			logger.info("putObjectResult.getETag(): "+putObjectResult.getETag());
			logger.info("putObjectResult.getMetadata().getETag(): "+putObjectResult.getMetadata().getETag());
			logger.info("putObjectResult.getContentMd5(): "+putObjectResult.getContentMd5());
			logger.info("putObjectResult.getMetadata().getContentMD5(): "+putObjectResult.getMetadata().getContentMD5());
		} catch (AmazonServiceException e) {
			logger.error(e.getErrorMessage());
			return false;
		}
		System.out.println("Done!");
		return true;
	}

	private List<File> listFilesToUpload(String pathUploadSrc, boolean recursive){
		List<File> files = new ArrayList<File>();
		int depth = 0;
		File file = new File(pathUploadSrc);
		if (file.isDirectory()){
			File[] listFiles = file.listFiles();
			for (File f : listFiles) {
				if (!f.isDirectory())
					files.add(f);
			}
		}
		else
			files.add(file);
		return files;
	}
}
