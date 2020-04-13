import javax.inject._
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import play.filters.gzip.GzipFilter

@Singleton
class Filters @Inject() (
  cors    : CORSFilter,
  logging : LoggingFilter,
  gzip    : GzipFilter) extends DefaultHttpFilters(logging, cors, gzip)
{}
