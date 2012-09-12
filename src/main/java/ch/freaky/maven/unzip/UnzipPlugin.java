package ch.freaky.maven.unzip;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * This is the mojo class for the 'unzip' goal.
 *
 * @goal unzip
 * @requiresOnline false
 * @requiresProject false
 * @author little.freaky
 */
public class UnzipPlugin extends AbstractMojo
{
  /**
   * The source file (.zip) to unzip to the destination
   *
   * @parameter
   */
  String sourceFile;
  
  /**
   * The destination directory to unzip the sourceFile to. If the destination
   * directory does not exist, it will be created.
   *
   * @parameter
   */
  String destinationDir;

  /**
   * Execute the unzip goal. This method will unzip the data in the zip-file
   * referenced by <source>sourceFile</source> to the directory referenced by
   * <source>destinationDir</source>
   * 
   * @throws MojoExecutionException If the source file does not exist, the 
   *          destination directory cannot be created or an error occurs during
   *          the unzip process.
   */
  public void execute() throws MojoExecutionException
  {
    validateSourceFile();
    validateDestinationDir();

    unzipContent();
  }
  
  /**
   * Check that the sourceFile parameter is correctly set and points to
   * an existing file
   * 
   * @throws MojoExecutionException 
   */
  private void validateSourceFile() throws MojoExecutionException
  {
    if ( sourceFile == null ) {
      throw new MojoExecutionException( "Parameter 'sourceFile' not specified. Please configure it in the 'configuration' section of the maven-unzip-plugin" );
    }
    
    if ( Files.notExists( new File(sourceFile).toPath() ) ) {
      throw new MojoExecutionException( "The sourceFile " + sourceFile + " cannot be found" );
    }
  }
  
  /**
   * Check that the destinationDir parameter is correctly set. If the 
   * directory does not exist, try to create it.
   * 
   * @throws MojoExecutionException 
   */
  private void validateDestinationDir() throws MojoExecutionException
  {
    if ( destinationDir == null ) {
      throw new MojoExecutionException( "Parameter 'destinationDir' not specified. Please configure it in the 'configuration' section of the maven-unzip-plugin" );
    }
    
    File destDirectory = new File(destinationDir);
    if ( destDirectory.exists() && destDirectory.isFile() ) {
      throw new MojoExecutionException( "The destination directory points to a file: " + destinationDir );
    }
    else if ( !destDirectory.exists() ) {
      boolean creationSuccessful = destDirectory.mkdirs();
      if ( !creationSuccessful ) {
        throw new MojoExecutionException( "Cannot create destination directory " + destinationDir );
      }
    }
  }
  
  /**
   * Unzip the contents of the sourceFile zip to the destination directory
   * 
   * @throws MojoExecutionException 
   */
  private void unzipContent() throws MojoExecutionException
  {
    Path sourcePath = new File(sourceFile).toPath();
    FileSystem zipFileSystem = null;
    
    try {
      zipFileSystem = FileSystems.newFileSystem(sourcePath, null);
      
      for ( Path rootDir : zipFileSystem.getRootDirectories() ) {
        copyFiles(rootDir, new File(destinationDir));
      }
    }
    catch ( IOException ioEx ) {
      throw new MojoExecutionException("Error while unziping sourceFile " + sourceFile, ioEx);
    }
    finally {
      try {
        zipFileSystem.close();
      }
      catch (IOException ex) {
        getLog().error( "Cannot close zipfile-system", ex );
      }
    }
  }
  
  /**
   * Copy all files from the given path to the destinationDir preserving
   * the directory structure and file permissions. 
   * 
   * NOTE: The files permissions are not copied correctly. It seems the zip file
   *       filesystem does not provide information about the executable attribute
   * 
   * @param fileSystem
   * @param destinationDir
   * @throws IOException 
   */
  private void copyFiles( Path directory, File destinationDir ) throws IOException
  {
    DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory);
    
    for ( Path sourcePath : directoryStream ) {
      File destinationPath = new File( destinationDir, sourcePath.getFileName().toString() );
      
      if ( Files.isDirectory(sourcePath) ) {
        //getLog().debug("Create dir " + destinationPath);
        if ( !destinationPath.exists() && !destinationPath.mkdir() ) {
          throw new IOException( "Cannot create directory " + destinationPath );
        }
        copyFiles( sourcePath, destinationPath );
      }
      else {
        //getLog().debug("Copy file " + sourcePath + " to " + destinationDir);
        Files.copy(sourcePath, destinationPath.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING );
      }
    }
  }
}
