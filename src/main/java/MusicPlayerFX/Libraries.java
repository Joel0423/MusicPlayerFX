/*
 * Repository link- https://github.com/Joel0423/upskill_campus/
 * File link-       https://github.com/Joel0423/upskill_campus/blob/master/src/main/java/MusicPlayerFX/Libraries.java
 */

package MusicPlayerFX;

public class Libraries
{
    private String folderpath;
    private String folder;

    public Libraries(String folder, String folderpath)
    {
        this.folderpath = folderpath;
        this.folder = folder;
    }

    public String getFolderpath()
    {
        return folderpath;
    }
    public String getFolder()
    {
        return folder;
    }

    @Override
    public String toString()
    {
        return this.getFolder();
    }
}
