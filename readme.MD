Example project implementing an Android DocumentsProvider.

#LocalDocumentsProvider

The LocalDocumentsProvider creates some static files and folders to demonstrate the methods which need to be implemented. See the documentation
in the class itself.

#SimulateNetworkDocumentsProvider

The SimulateNetworkDocumentsProvider simulates calling the network for loading documents.

At the moment switching between the two implementations can be done by enabling the correct implementation in the manifest.

## Resources

https://medium.com/androiddevelopers/building-a-documentsprovider-f7f2fb38e86a#.k0ufgiu1l?utm_campaign=android_series_adp_documentsprovider_blog_090716&utm_source=anddev&utm_medium=yt-desc
https://developer.android.com/guide/topics/providers/create-document-provider
https://www.dropboxforum.com/t5/Dropbox-API-Support-Feedback/Android-Storage-Access-Framework-support/td-p/147405/page/2
https://github.com/google/samba-documents-provider

StackOverflow:

https://stackoverflow.com/questions/20201412/android-documentsprovider-opendocument-is-called-on-the-main-thread
https://stackoverflow.com/questions/40736062/problems-with-implementing-documentsprovider-for-the-cloud-on-android
https://stackoverflow.com/questions/62302642/correctly-implementing-opendocument-for-a-documentsprovider